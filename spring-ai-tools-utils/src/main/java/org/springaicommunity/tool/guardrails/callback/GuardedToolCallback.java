package org.springaicommunity.tool.guardrails.callback;

import org.springaicommunity.tool.guardrails.exception.GuardrailViolationException;
import org.springaicommunity.tool.guardrails.input.InputGuardrailResult;
import org.springaicommunity.tool.guardrails.input.ToolInputGuardrail;
import org.springaicommunity.tool.guardrails.output.OutputGuardrailResult;
import org.springaicommunity.tool.guardrails.output.ToolOutputGuardrail;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A {@link ToolCallback} decorator that runs a chain of {@link ToolInputGuardrail}s
 * before delegating to the wrapped callback and a chain of
 * {@link ToolOutputGuardrail}s on the result.
 * <p>
 * Instances are created through the fluent {@link Builder} returned by
 * {@link #wrap(ToolCallback)}.
 * </p>
 */
public class GuardedToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final List<ToolInputGuardrail> inputGuardrails;
    private final List<ToolOutputGuardrail> outputGuardrails;

    private GuardedToolCallback(Builder builder) {
        this.delegate = builder.delegate;
        this.inputGuardrails = List.copyOf(builder.inputGuardrails);
        this.outputGuardrails = List.copyOf(builder.outputGuardrails);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    /**
     * Runs input guardrails, delegates to the wrapped callback, then runs output
     * guardrails. Throws {@link GuardrailViolationException} if any guardrail blocks
     * the input or output.
     *
     * @throws GuardrailViolationException if an input or output guardrail is violated
     */
    @Override
    public String call(String toolInput, ToolContext toolContext) {
        ToolDefinition def = delegate.getToolDefinition();

        String resolvedInput = toolInput;
        for (ToolInputGuardrail guardrail : inputGuardrails) {
            InputGuardrailResult result = guardrail.evaluate(def, resolvedInput);
            if (!result.isPass()) {
                throw new GuardrailViolationException(
                    def.name(),
                    resolvedInput,
                    result.failureMessage()
                );
            }
            resolvedInput = result.sanitizedInput();
        }

        String resolvedOutput = toolContext != null
            ? delegate.call(resolvedInput, toolContext)
            : delegate.call(resolvedInput);

        for (ToolOutputGuardrail guardrail : outputGuardrails) {
            OutputGuardrailResult result = guardrail.evaluate(def, resolvedOutput);
            if (!result.isPass()) {
                throw new GuardrailViolationException(
                    def.name(),
                    resolvedOutput,
                    result.failureMessage()
                );
            }
            resolvedOutput = result.sanitizedOutput();
        }

        return resolvedOutput;
    }

    /**
     * Begins construction of a {@link GuardedToolCallback} wrapping the given delegate.
     *
     * @param delegate the callback to wrap
     * @return a new {@link Builder}
     */
    public static Builder wrap(ToolCallback delegate) {
        return new Builder(delegate);
    }

    /**
     * Fluent builder for {@link GuardedToolCallback}.
     */
    public static class Builder {

        private final ToolCallback delegate;
        private final List<ToolInputGuardrail> inputGuardrails = new ArrayList<>();
        private final List<ToolOutputGuardrail> outputGuardrails = new ArrayList<>();

        private Builder(ToolCallback delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        }

        /**
         * Appends an input guardrail to the chain.
         *
         * @param guardrail the input guardrail to add
         * @return this builder
         */
        public Builder inputGuardrail(ToolInputGuardrail guardrail) {
            this.inputGuardrails.add(guardrail);
            return this;
        }

        /**
         * Appends an output guardrail to the chain.
         *
         * @param guardrail the output guardrail to add
         * @return this builder
         */
        public Builder outputGuardrail(ToolOutputGuardrail guardrail) {
            this.outputGuardrails.add(guardrail);
            return this;
        }

        /**
         * Builds the {@link GuardedToolCallback}.
         *
         * @return the configured {@link GuardedToolCallback}
         * @throws IllegalStateException if no guardrails have been added
         */
        public GuardedToolCallback build() {
            if (inputGuardrails.isEmpty() && outputGuardrails.isEmpty()) {
                throw new IllegalStateException(
                        "GuardedToolCallback requires at least one input or output guardrail"
                );
            }
            return new GuardedToolCallback(this);
        }
    }

}