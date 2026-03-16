package org.springaicommunity.tool.confirmation;

/**
 * A {@link ConfirmationHandler} that unconditionally approves every confirmation
 * request without any user interaction.
 * <p>
 * Useful for testing or for tools that should never be blocked by the confirmation
 * layer. Use the singleton {@link #INSTANCE} rather than constructing a new instance.
 * </p>
 */
public class AutoApproveConfirmationHandler implements ConfirmationHandler {

    /** Shared singleton instance. */
    public static final AutoApproveConfirmationHandler INSTANCE =
            new AutoApproveConfirmationHandler();

    private AutoApproveConfirmationHandler() {}

    @Override
    public ConfirmationResult confirm(ConfirmationRequest request) {
        return ConfirmationResult.approved();
    }
}
