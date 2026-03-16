package org.springaicommunity.tool.guardrails.exception;

/**
 * Thrown when an input or output guardrail blocks a tool invocation.
 */
public class GuardrailViolationException extends RuntimeException {

    private final String toolName;
    private final String toolInput;

    /**
     * Constructs a new exception with a formatted message incorporating the tool name
     * and reason for the violation.
     *
     * @param toolName  the name of the tool that was blocked
     * @param toolInput the raw input that triggered the violation
     * @param reason    human-readable description of the guardrail violation
     */
    public GuardrailViolationException(String toolName, String toolInput, String reason) {
        super("Guardrail violation on tool '%s': %s".formatted(toolName, reason));
        this.toolName = toolName;
        this.toolInput = toolInput;
    }

    /**
     * Returns the name of the tool that was blocked.
     *
     * @return the tool name
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * Returns the raw tool input that triggered the violation.
     *
     * @return the raw input string
     */
    public String getToolInput() {
        return toolInput;
    }
}
