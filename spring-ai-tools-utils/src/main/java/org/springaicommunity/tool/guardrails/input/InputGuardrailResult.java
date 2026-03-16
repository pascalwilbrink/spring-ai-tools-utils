package org.springaicommunity.tool.guardrails.input;

/**
 * Outcome of a {@link ToolInputGuardrail} evaluation.
 * <p>
 * A passing result carries the (possibly sanitized) input to pass to the next layer;
 * a blocked result carries a human-readable failure message.
 * </p>
 *
 * @param status         whether the input was accepted or blocked
 * @param sanitizedInput the input to propagate when the status is {@link Status#PASS}; {@code null} when blocked
 * @param failureMessage the reason the input was blocked; {@code null} when passing
 */
public record InputGuardrailResult(
        Status status,
        String sanitizedInput,
        String failureMessage
) {
    /** Possible outcomes of an input guardrail check. */
    public enum Status {
        /** The input was accepted and may proceed. */
        PASS,
        /** The input was rejected and must not proceed. */
        BLOCKED
    }

    /**
     * Creates a passing result carrying the given (possibly sanitized) input.
     *
     * @param input the input string to propagate to the next processing step
     * @return a passing {@link InputGuardrailResult}
     */
    public static InputGuardrailResult pass(String input) {
        return new InputGuardrailResult(Status.PASS, input, null);
    }

    /**
     * Creates a blocked result with the given failure message.
     *
     * @param reason human-readable explanation of why the input was blocked
     * @return a blocked {@link InputGuardrailResult}
     */
    public static InputGuardrailResult blocked(String reason) {
        return new InputGuardrailResult(Status.BLOCKED, null, reason);
    }

    /**
     * Returns {@code true} when the guardrail passed the input without blocking it.
     *
     * @return {@code true} if status is {@link Status#PASS}
     */
    public boolean isPass() {
        return status == Status.PASS;
    }
}
