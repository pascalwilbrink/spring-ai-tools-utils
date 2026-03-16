package org.springaicommunity.tool.confirmation;

/**
 * Outcome of a {@link ConfirmationHandler#confirm(ConfirmationRequest)} call.
 *
 * @param status           whether the tool invocation was approved or rejected
 * @param rejectionMessage the reason for rejection; {@code null} when approved
 */
public record ConfirmationResult(
        Status status,
        String rejectionMessage
) {
    /** Possible outcomes of a confirmation decision. */
    public enum Status {
        /** The tool invocation was approved and may proceed. */
        APPROVED,
        /** The tool invocation was rejected. */
        REJECTED
    }

    /**
     * Creates an approved confirmation result.
     *
     * @return an approved {@link ConfirmationResult}
     */
    public static ConfirmationResult approved() {
        return new ConfirmationResult(Status.APPROVED, null);
    }

    /**
     * Creates a rejected confirmation result.
     *
     * @param reason human-readable explanation of the rejection
     * @return a rejected {@link ConfirmationResult}
     */
    public static ConfirmationResult rejected(String reason) {
        return new ConfirmationResult(Status.REJECTED, reason);
    }

    /**
     * Returns {@code true} when the confirmation was approved.
     *
     * @return {@code true} if status is {@link Status#APPROVED}
     */
    public boolean isApproved() {
        return status == Status.APPROVED;
    }
}
