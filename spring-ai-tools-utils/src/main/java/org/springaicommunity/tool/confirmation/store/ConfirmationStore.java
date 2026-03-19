package org.springaicommunity.tool.confirmation.store;

import java.util.Optional;

/**
 * Repository for {@link PendingConfirmation} records while a tool-invocation
 * confirmation is in progress.
 */
public interface ConfirmationStore {

    /**
     * Persists a pending confirmation record.
     *
     * @param pendingConfirmation the confirmation to store
     */
    void save(PendingConfirmation pendingConfirmation);

    /**
     * Looks up a pending confirmation by its unique identifier.
     *
     * @param confirmationId the identifier assigned when the confirmation was stored
     * @return the pending confirmation, or empty if not found
     */
    Optional<PendingConfirmation> findById(String confirmationId);

    /**
     * Removes the pending confirmation with the given identifier.
     *
     * @param confirmationId the identifier of the confirmation to remove
     */
    void remove(String confirmationId);
}
