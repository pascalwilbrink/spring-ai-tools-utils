package org.springaicommunity.tool.guardrails.input;

import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Strategy for inspecting and optionally sanitizing a tool's input before execution.
 * <p>
 * Implementations may pass the input through (possibly modified), or block it by
 * returning a {@link InputGuardrailResult#blocked(String) blocked} result.
 * </p>
 */
@FunctionalInterface
public interface ToolInputGuardrail {

    /**
     * Evaluates the given tool input against this guardrail's policy.
     *
     * @param toolDefinition metadata about the tool being invoked
     * @param toolInput      the raw JSON input string for the tool
     * @return a result indicating whether the input is permitted and carrying the
     *         (possibly sanitized) input or a failure message
     */
    InputGuardrailResult evaluate(ToolDefinition toolDefinition, String toolInput);

}