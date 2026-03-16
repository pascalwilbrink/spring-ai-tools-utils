package org.springaicommunity.tool.guardrails.output;

import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Strategy for inspecting and optionally sanitizing a tool's output after execution.
 * <p>
 * Implementations may pass the output through (possibly modified), or block it by
 * returning an {@link OutputGuardrailResult#blocked(String) blocked} result.
 * </p>
 */
@FunctionalInterface
public interface ToolOutputGuardrail {

    /**
     * Evaluates the given tool output against this guardrail's policy.
     *
     * @param toolDefinition metadata about the tool that produced the output
     * @param toolOutput     the raw output string produced by the tool
     * @return a result indicating whether the output is permitted and carrying the
     *         (possibly sanitized) output or a failure message
     */
    OutputGuardrailResult evaluate(ToolDefinition toolDefinition, String toolOutput);
}