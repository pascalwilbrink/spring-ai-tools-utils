package org.springaicommunity.tool.guardrails.builtin;

import org.springaicommunity.tool.guardrails.output.OutputGuardrailResult;
import org.springaicommunity.tool.guardrails.output.ToolOutputGuardrail;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.nio.charset.StandardCharsets;

/**
 * Output guardrail that rejects tool outputs whose UTF-8 byte length exceeds a
 * configurable maximum. Defaults to {@value #DEFAULT_MAX_BYTES} bytes.
 */
public class MaxOutputSizeOutputGuardrail implements ToolOutputGuardrail {

    private static final int DEFAULT_MAX_BYTES = 8192;

    private final int maxBytes;

    /**
     * Creates an instance using the default maximum of {@value #DEFAULT_MAX_BYTES} bytes.
     */
    public MaxOutputSizeOutputGuardrail() {
        this(DEFAULT_MAX_BYTES);
    }

    /**
     * Creates an instance with the given byte limit.
     *
     * @param maxBytes maximum number of UTF-8 bytes allowed in the tool output
     */
    public MaxOutputSizeOutputGuardrail(int maxBytes) {
        this.maxBytes = maxBytes;
    }

    @Override
    public OutputGuardrailResult evaluate(ToolDefinition def, String toolOutput) {
        int size = toolOutput.getBytes(StandardCharsets.UTF_8).length;
        if (size > maxBytes) {
            return OutputGuardrailResult.blocked(
            "Output size %d bytes exceeds maximum allowed %d bytes for tool '%s'"
                    .formatted(
                        size,
                        maxBytes,
                        def.name()
                    )
            );
        }
        return OutputGuardrailResult.pass(toolOutput);
    }

}
