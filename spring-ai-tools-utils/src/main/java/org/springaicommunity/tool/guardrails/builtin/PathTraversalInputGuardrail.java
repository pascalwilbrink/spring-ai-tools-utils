package org.springaicommunity.tool.guardrails.builtin;

import org.springaicommunity.tool.guardrails.input.InputGuardrailResult;
import org.springaicommunity.tool.guardrails.input.ToolInputGuardrail;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Input guardrail that detects and blocks path-traversal attempts by checking for
 * the {@code ..} sequence in the tool input.
 */
public class PathTraversalInputGuardrail implements ToolInputGuardrail {

    @Override
    public InputGuardrailResult evaluate(ToolDefinition def, String toolInput) {
        if (toolInput.contains("..")) {
            return InputGuardrailResult.blocked(
                "Path traversal detected in input for tool '%s'"
                    .formatted(def.name())
            );
        }
        return InputGuardrailResult.pass(toolInput);
    }

}
