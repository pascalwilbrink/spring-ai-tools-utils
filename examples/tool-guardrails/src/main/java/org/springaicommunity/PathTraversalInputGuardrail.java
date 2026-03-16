package org.springaicommunity;

import org.springaicommunity.tool.guardrails.input.InputGuardrailResult;
import org.springaicommunity.tool.guardrails.input.ToolInputGuardrail;
import org.springframework.ai.tool.definition.ToolDefinition;

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