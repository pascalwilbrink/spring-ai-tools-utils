package org.springaicommunity.tool.guardrails.builtin;

import org.springaicommunity.tool.guardrails.input.InputGuardrailResult;
import org.springaicommunity.tool.guardrails.input.ToolInputGuardrail;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;

/**
 * Input guardrail that detects common SQL injection patterns by checking for
 * destructive or execution-related SQL keywords in the tool input.
 */
public class SqlInjectionInputGuardrail implements ToolInputGuardrail {

    private static final List<String> BLOCKED_KEYWORDS = List.of(
        "drop", "truncate", "delete", "insert", "update", "alter",
        "create", "exec", "execute", "xp_", "sp_", "--","/*", "*/"
    );

    @Override
    public InputGuardrailResult evaluate(ToolDefinition def, String toolInput) {
        String lower = toolInput.toLowerCase();
        return BLOCKED_KEYWORDS.stream()
            .filter(lower::contains)
            .findFirst()
            .map(keyword -> InputGuardrailResult.blocked(
                "SQL Injection detected in input for tool '%s': blocked keyword '%s'"
                    .formatted(
                        def.name(),
                        keyword
                    )
            )).orElse(InputGuardrailResult.pass(toolInput));
    }

}
