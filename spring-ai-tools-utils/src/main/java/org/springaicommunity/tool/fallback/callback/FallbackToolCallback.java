package org.springaicommunity.tool.fallback.callback;

import org.springaicommunity.tool.fallback.strategy.FallbackToolStrategy;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Objects;

/**
 * A {@link ToolCallback} decorator that catches any exception thrown by the wrapped
 * callback and delegates to a {@link FallbackToolStrategy} to produce an alternative
 * response.
 * <p>
 * Instances are created through the fluent {@link Builder} returned by
 * {@link #wrap(ToolCallback)}.
 * </p>
 */
public class FallbackToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final FallbackToolStrategy fallbackToolStrategy;

    private FallbackToolCallback(Builder builder) {
        this.delegate = builder.delegate;
        this.fallbackToolStrategy = builder.fallbackToolStrategy;
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

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        try {
            return toolContext != null
                    ? delegate.call(toolInput, toolContext)
                    : delegate.call(toolInput);
        } catch (Throwable t) {
            return fallbackToolStrategy.fallback(toolInput, t);
        }
    }

    /**
     * Begins construction of a {@link FallbackToolCallback} wrapping the given delegate.
     *
     * @param delegate the callback to wrap
     * @return a new {@link Builder}
     */
    public static Builder wrap(ToolCallback delegate) {
        return new Builder(delegate);
    }

    /**
     * Fluent builder for {@link FallbackToolCallback}.
     */
    public static class Builder {

        private final ToolCallback delegate;
        private FallbackToolStrategy fallbackToolStrategy;

        private Builder(ToolCallback delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        }

        /**
         * Sets the fallback strategy to invoke when the delegate throws.
         *
         * @param strategy the fallback strategy to use
         * @return this builder
         */
        public Builder fallback(FallbackToolStrategy strategy) {
            this.fallbackToolStrategy = strategy;
            return this;
        }

        /**
         * Builds the {@link FallbackToolCallback}.
         *
         * @return the configured {@link FallbackToolCallback}
         * @throws NullPointerException if no {@link FallbackToolStrategy} has been set
         */
        public FallbackToolCallback build() {
            Objects.requireNonNull(fallbackToolStrategy, "FallbackToolStrategy must not be null");
            return new FallbackToolCallback(this);
        }
    }
}
