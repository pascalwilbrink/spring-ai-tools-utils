package org.springaicommunity.tool.guardrails.builtin;

import org.springaicommunity.tool.guardrails.output.OutputGuardrailResult;
import org.springaicommunity.tool.guardrails.output.ToolOutputGuardrail;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.regex.Pattern;

/**
 * Output guardrail that redacts credential-like key-value pairs from tool output.
 * Fields matched by keywords such as {@code password}, {@code secret}, {@code token},
 * or {@code api_key} have their values replaced with {@code [REDACTED]}.
 */
public class SensitiveDataOutputGuardrail implements ToolOutputGuardrail {

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
        "(?i)(password|secret|token|api[_-]?key|access[_-]?key|private[_-]?key)" +
        "\\s*[:=]\\s*\\S+"
    );

    @Override
    public OutputGuardrailResult evaluate(ToolDefinition def, String toolOutput) {
        String redacted = SENSITIVE_PATTERN.matcher(toolOutput)
                .replaceAll(match -> match.group(1) + ": [REDACTED]");
        return OutputGuardrailResult.pass(redacted);
    }

}
