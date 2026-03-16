package org.springaicommunity.tool.guardrails.builtin;

import org.springaicommunity.tool.guardrails.input.InputGuardrailResult;
import org.springaicommunity.tool.guardrails.input.ToolInputGuardrail;
import org.springaicommunity.tool.guardrails.output.OutputGuardrailResult;
import org.springaicommunity.tool.guardrails.output.ToolOutputGuardrail;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Factory class providing ready-made {@link ToolInputGuardrail} and
 * {@link ToolOutputGuardrail} implementations for common security and size-limiting
 * scenarios.
 */
public final class Guardrails {

    private Guardrails() { }

    /**
     * Returns a guardrail that rejects inputs containing path-traversal sequences
     * such as {@code ..}.
     *
     * @return a {@link ToolInputGuardrail} that blocks path-traversal attempts
     */
    public static ToolInputGuardrail pathTraversal() {
        return new PathTraversalInputGuardrail();
    }

    /**
     * Returns a guardrail that rejects inputs containing common SQL-injection keywords
     * such as {@code DROP}, {@code DELETE}, or comment markers.
     *
     * @return a {@link ToolInputGuardrail} that blocks SQL injection attempts
     */
    public static ToolInputGuardrail sqlInjection() {
        return new SqlInjectionInputGuardrail();
    }

    /**
     * Returns a guardrail that blocks inputs exceeding the given byte limit (UTF-8).
     *
     * @param maxBytes maximum number of UTF-8 bytes allowed in the tool input
     * @return a {@link ToolInputGuardrail} that enforces the byte limit
     */
    public static ToolInputGuardrail maxInputSize(int maxBytes) {
        return new MaxInputSizeInputGuardrail(maxBytes);
    }

    /**
     * Returns a guardrail that blocks inputs containing any of the supplied keywords
     * (case-insensitive).
     *
     * @param keywords list of substrings whose presence should cause the input to be blocked
     * @return a {@link ToolInputGuardrail} that blocks inputs with forbidden keywords
     */
    public static ToolInputGuardrail blockKeywords(List<String> keywords) {
        return (def, input) -> {
            String lower = input.toLowerCase();
            return keywords.stream()
                .filter(lower::contains)
                .findFirst()
                .map(k -> InputGuardrailResult.blocked(
                    "Blocked keyword '%s' found in input for tool '%s'"
                        .formatted(
                            k,
                            def.name()
                        )
                ))
                .orElse(InputGuardrailResult.pass(input));
        };
    }

    /**
     * Returns a guardrail that only allows execution of tools whose names are in the
     * given allow-list, blocking all others.
     *
     * @param toolNames set of tool names that are permitted to execute
     * @return a {@link ToolInputGuardrail} that enforces the allow-list
     */
    public static ToolInputGuardrail allowedTools(Set<String> toolNames) {
        return (def, input) -> toolNames.contains(def.name())
            ? InputGuardrailResult.pass(input)
            : InputGuardrailResult.blocked(
                "Tool '%s' is not in the allowed list"
                .formatted(
                    def.name()
                )
        );
    }

    /**
     * Returns a guardrail that redacts common sensitive-data patterns (passwords,
     * tokens, API keys, etc.) from tool output by replacing the value portion with
     * {@code [REDACTED]}.
     *
     * @return a {@link ToolOutputGuardrail} that redacts sensitive key-value pairs
     */
    public static ToolOutputGuardrail sensitiveData() {
        return new SensitiveDataOutputGuardrail();
    }

    /**
     * Returns a guardrail that blocks outputs exceeding the given byte limit (UTF-8).
     *
     * @param maxBytes maximum number of UTF-8 bytes allowed in the tool output
     * @return a {@link ToolOutputGuardrail} that enforces the byte limit
     */
    public static ToolOutputGuardrail maxOutputSize(int maxBytes) {
        return new MaxOutputSizeOutputGuardrail(maxBytes);
    }

    /**
     * Returns a guardrail that replaces all occurrences of the given compiled pattern
     * in the tool output with the provided replacement string.
     *
     * @param pattern     the compiled regex pattern to match
     * @param replacement the replacement string (supports regex back-references)
     * @return a {@link ToolOutputGuardrail} that applies the redaction
     */
    public static ToolOutputGuardrail redactPattern(Pattern pattern, String replacement) {
        return (def, output) -> OutputGuardrailResult.pass(
                pattern.matcher(output).replaceAll(replacement)
        );
    }

    /**
     * Returns a guardrail that replaces all occurrences of the given regex string
     * in the tool output with the provided replacement string.
     *
     * @param regex       the regular-expression pattern to match
     * @param replacement the replacement string (supports regex back-references)
     * @return a {@link ToolOutputGuardrail} that applies the redaction
     */
    public static ToolOutputGuardrail redactPattern(String regex, String replacement) {
        return redactPattern(Pattern.compile(regex), replacement);
    }
}
