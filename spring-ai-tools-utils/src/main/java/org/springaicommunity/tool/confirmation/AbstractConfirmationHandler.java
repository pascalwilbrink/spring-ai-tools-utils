package org.springaicommunity.tool.confirmation;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springaicommunity.tool.confirmation.properties.ConfirmationProperties;
import org.springaicommunity.tool.confirmation.store.ConfirmationStore;
import org.springaicommunity.tool.confirmation.store.PendingConfirmation;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Base implementation of {@link ConfirmationHandler} that persists pending
 * confirmations in a {@link ConfirmationStore}, waits asynchronously for a response
 * via {@link CompletableFuture}, and times out according to
 * {@link ConfirmationProperties}.
 * <p>
 * Subclasses implement {@link #onPendingConfirmation} to notify the end-user through
 * an appropriate transport (WebSocket, SSE, etc.).
 * </p>
 */
public abstract class AbstractConfirmationHandler implements ConfirmationHandler {

    private final ConfirmationStore confirmationStore;
    private final Cache<String, CompletableFuture<ConfirmationResult>> futures;
    private final ConfirmationProperties properties;

    /**
     * Constructs the handler with the given store and properties.
     *
     * @param confirmationStore persistent store for pending confirmations
     * @param properties        configuration properties controlling timeout and capacity
     */
    protected AbstractConfirmationHandler(
        final ConfirmationStore confirmationStore,
        final ConfirmationProperties properties) {

        this.confirmationStore = confirmationStore;
        this.properties = properties;
        this.futures = Caffeine.newBuilder()
            .maximumSize(properties.maxPending())
            .build();
    }

    @Override
    public ConfirmationResult confirm(ConfirmationRequest request) {
        String confirmationId = UUID.randomUUID().toString();
        CompletableFuture<ConfirmationResult> future = new CompletableFuture<>();

        confirmationStore.save(PendingConfirmation.of(confirmationId, request));
        futures.put(confirmationId, future);

        onPendingConfirmation(confirmationId, request);

        try {
            return future.orTimeout(properties.timeout().toSeconds(), TimeUnit.SECONDS)
                    .exceptionally(ex -> ConfirmationResult.rejected("Timed out"))
                    .join();
        } finally {
            futures.invalidate(confirmationId);
            confirmationStore.remove(confirmationId);
        }
    }

    /**
     * Resolves a pending confirmation identified by {@code confirmationId}.
     *
     * @param confirmationId the identifier returned when the confirmation was stored
     * @param approved       {@code true} to approve the tool invocation
     * @param reason         the rejection reason; required when {@code approved} is {@code false}
     * @throws IllegalArgumentException if {@code approved} is {@code false} and {@code reason}
     *                                  is blank, or if no pending confirmation exists for the id
     */
    public void respond(
        String confirmationId,
        boolean approved,
        String reason
    ) {
        if (!approved && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("reason must be provided when rejecting a confirmation");
        }
        CompletableFuture<ConfirmationResult> future = futures.getIfPresent(confirmationId);
        if (future == null) {
            throw new IllegalArgumentException(
                "No pending confirmation with id: " + confirmationId
            );
        }
        future.complete(approved
            ? ConfirmationResult.approved()
            : ConfirmationResult.rejected(reason));
    }

    /**
     * Returns {@code true} if a pending confirmation with the given id is still
     * awaiting a response.
     *
     * @param confirmationId the identifier to check
     * @return {@code true} if a future for the id exists in the in-memory cache
     */
    public boolean hasPending(String confirmationId) {
        return futures.getIfPresent(confirmationId) != null;
    }

    /**
     * Called when a confirmation is pending.
     * Implement this to push the confirmation to the user
     * via WebSocket, SSE, or any other transport.
     *
     * @param confirmationId unique identifier for this pending confirmation
     * @param request        details about the tool invocation awaiting approval
     */
    protected abstract void onPendingConfirmation(
        String confirmationId,
        ConfirmationRequest request
    );
}
