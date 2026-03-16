package org.springaicommunity.tool.guardrails.builtin;


import org.springaicommunity.tool.guardrails.input.InputGuardrailResult;
import org.springaicommunity.tool.guardrails.input.ToolInputGuardrail;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.nio.charset.StandardCharsets;

/**
 * Input guardrail that rejects tool inputs whose UTF-8 byte length exceeds a
 * configurable maximum. Defaults to {@value #DEFAULT_MAX_BYTES} bytes.
 */
public class MaxInputSizeInputGuardrail implements ToolInputGuardrail {

    private static final int DEFAULT_MAX_BYTES = 4096;

    private final int maxBytes;

    /**
     * Creates an instance using the default maximum of {@value #DEFAULT_MAX_BYTES} bytes.
     */
    public MaxInputSizeInputGuardrail() {
        this(DEFAULT_MAX_BYTES);
    }

    /**
     * Creates an instance with the given byte limit.
     *
     * @param maxBytes maximum number of UTF-8 bytes allowed in the tool input
     */
    public MaxInputSizeInputGuardrail(int maxBytes) {
        this.maxBytes = maxBytes;
    }

    @Override
    public InputGuardrailResult evaluate(ToolDefinition def, String toolInput) {
        int size = toolInput.getBytes(StandardCharsets.UTF_8).length;
        if (size > maxBytes) {
            return InputGuardrailResult.blocked(
                "Input size %d bytes exceeds maximum allowed %d bytes for tool '%s'"
                    .formatted(
                        size,
                        maxBytes,
                        def.name()
                    )
            );
        }
        return InputGuardrailResult.pass(toolInput);
    }

}
