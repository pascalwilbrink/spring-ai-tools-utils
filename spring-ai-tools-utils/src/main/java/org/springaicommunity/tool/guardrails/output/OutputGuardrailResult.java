package org.springaicommunity.tool.guardrails.output;

/**
 * Outcome of a {@link ToolOutputGuardrail} evaluation.
 * <p>
 * A passing result carries the (possibly sanitized) output to return to the caller;
 * a blocked result carries a human-readable failure message.
 * </p>
 *
 * @param status          whether the output was accepted or blocked
 * @param sanitizedOutput the output to propagate when the status is {@link Status#PASS}; {@code null} when blocked
 * @param failureMessage  the reason the output was blocked; {@code null} when passing
 */
public record OutputGuardrailResult(
        Status status,
        String sanitizedOutput,
        String failureMessage
) {
    /** Possible outcomes of an output guardrail check. */
    public enum Status {
        /** The output was accepted and may be returned to the caller. */
        PASS,
        /** The output was rejected and must not be returned. */
        BLOCKED
    }

    /**
     * Creates a passing result carrying the given (possibly sanitized) output.
     *
     * @param output the output string to propagate to the caller
     * @return a passing {@link OutputGuardrailResult}
     */
    public static OutputGuardrailResult pass(String output) {
        return new OutputGuardrailResult(Status.PASS, output, null);
    }

    /**
     * Creates a blocked result with the given failure message.
     *
     * @param reason human-readable explanation of why the output was blocked
     * @return a blocked {@link OutputGuardrailResult}
     */
    public static OutputGuardrailResult blocked(String reason) {
        return new OutputGuardrailResult(Status.BLOCKED, null, reason);
    }

    /**
     * Returns {@code true} when the guardrail passed the output without blocking it.
     *
     * @return {@code true} if status is {@link Status#PASS}
     */
    public boolean isPass() {
        return status == Status.PASS;
    }
}
