package org.springaicommunity.tool.confirmation.store;

import org.springaicommunity.tool.confirmation.ConfirmationRequest;

import java.time.Instant;

/**
 * Immutable snapshot of a tool-invocation confirmation request that is stored while
 * awaiting a human response.
 *
 * @param confirmationId unique identifier assigned to this pending confirmation
 * @param toolName       name of the tool whose invocation is pending approval
 * @param toolInput      raw JSON input that was passed to the tool
 * @param createdAt      timestamp at which the confirmation was created
 */
public record PendingConfirmation(
    String confirmationId,
    String toolName,
    String toolInput,
    Instant createdAt
) {
    /**
     * Constructs a {@link PendingConfirmation} from a confirmation id and a
     * {@link ConfirmationRequest}, recording the current time as the creation timestamp.
     */
    public static PendingConfirmation of(
        String confirmationId,
        ConfirmationRequest request
    ) {
        return new PendingConfirmation(
            confirmationId,
            request.toolDefinition().name(),
            request.toolInput(),
            Instant.now()
        );
    }
}
