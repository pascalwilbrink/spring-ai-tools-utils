package org.springaicommunity.tool.confirmation.exception;

/**
 * Thrown when a {@link org.springaicommunity.tool.confirmation.ConfirmationHandler}
 * rejects a tool-invocation confirmation request.
 */
public class ToolRejectionException extends RuntimeException {

    private final String toolName;
    private final String toolInput;

    /**
     * Constructs a new exception with a formatted message incorporating the tool name
     * and the rejection reason.
     *
     * @param toolName  the name of the tool that was rejected
     * @param toolInput the raw input that was presented for approval
     * @param reason    human-readable explanation of the rejection
     */
    public ToolRejectionException(String toolName, String toolInput, String reason) {
        super("Tool '%s' was rejected: %s".formatted(toolName, reason));
        this.toolName = toolName;
        this.toolInput = toolInput;
    }

    /** Returns the name of the tool whose invocation was rejected. */
    public String getToolName() {
        return toolName;
    }

    /** Returns the raw tool input that was presented for approval. */
    public String getToolInput() {
        return toolInput;
    }
}