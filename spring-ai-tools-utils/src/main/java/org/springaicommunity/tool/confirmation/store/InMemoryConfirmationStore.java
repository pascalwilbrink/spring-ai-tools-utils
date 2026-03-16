package org.springaicommunity.tool.confirmation.store;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, in-memory implementation of {@link ConfirmationStore} backed by a
 * {@link java.util.concurrent.ConcurrentHashMap}.
 */
public class InMemoryConfirmationStore implements ConfirmationStore {

    private final Map<String, PendingConfirmation> store = new ConcurrentHashMap<>();

    @Override
    public void save(PendingConfirmation pendingConfirmation) {
        store.put(pendingConfirmation.confirmationId(), pendingConfirmation);
    }

    @Override
    public Optional<PendingConfirmation> findById(String confirmationId) {
        return Optional.ofNullable(store.get(confirmationId));
    }

    @Override
    public void remove(String confirmationId) {
        store.remove(confirmationId);
    }
}
