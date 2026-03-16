package org.springaicommunity.tool.confirmation;

/**
 * Strategy for obtaining human approval before a tool is allowed to execute.
 * <p>
 * Implementations may block until an external approval is received (e.g. via HTTP,
 * WebSocket, or CLI prompt) or immediately approve/reject based on policy.
 * </p>
 */
@FunctionalInterface
public interface ConfirmationHandler {

    /**
     * Requests confirmation for the described tool invocation.
     *
     * @param request details about the tool and its input
     * @return the result of the confirmation decision
     */
    ConfirmationResult confirm(ConfirmationRequest request);

}