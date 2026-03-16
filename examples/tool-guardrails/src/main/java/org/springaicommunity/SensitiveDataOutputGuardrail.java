package org.springaicommunity;

import org.springaicommunity.tool.guardrails.output.OutputGuardrailResult;
import org.springaicommunity.tool.guardrails.output.ToolOutputGuardrail;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.regex.Pattern;

public class SensitiveDataOutputGuardrail implements ToolOutputGuardrail {

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(?i)(password|secret|token|api[_-]?key)\\s*[:=]\\s*\\S+"
    );

    @Override
    public OutputGuardrailResult evaluate(ToolDefinition def, String toolOutput) {
        String redacted = SENSITIVE_PATTERN.matcher(toolOutput)
                .replaceAll(match -> match.group(1) + ": [REDACTED]");
        return OutputGuardrailResult.pass(redacted);
    }
}
