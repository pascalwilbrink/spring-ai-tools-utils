package org.springaicommunity.tool.retry.callback;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Objects;

/**
 * A {@link ToolCallback} decorator that retries the delegate on exception using
 * Spring Retry's {@link RetryTemplate}.
 *
 * <p>The number of retries is configured via {@link #maxRetries}: the delegate will
 * be called at most {@code maxRetries + 1} times (one initial attempt plus
 * {@code maxRetries} retries) before the last exception is rethrown.</p>
 */
public class RetryableToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final int maxRetries;
    private final RetryTemplate retryTemplate;

    private RetryableToolCallback(Builder builder) {
        this.delegate = builder.delegate;
        this.maxRetries = builder.maxRetries;
        this.retryTemplate = buildRetryTemplate(maxRetries);
    }

    private static RetryTemplate buildRetryTemplate(int maxRetries) {
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        // maxAttempts = initial attempt + retries
        retryPolicy.setMaxAttempts(maxRetries + 1);

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(1000L);

        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(retryPolicy);
        template.setBackOffPolicy(backOffPolicy);
        return template;
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
        return this.call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return retryTemplate.execute(context -> delegate.call(toolInput, toolContext));
    }

    /**
     * Creates a builder for wrapping the given delegate callback.
     *
     * @param delegate the callback to wrap
     * @return a new builder
     */
    public static Builder wrap(ToolCallback delegate) {
        return new Builder(delegate);
    }

    /**
     * Fluent builder for {@link RetryableToolCallback}.
     */
    public static class Builder {

        private final ToolCallback delegate;
        private int maxRetries;

        private Builder(ToolCallback delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        }

        /**
         * Sets the maximum number of retries after the initial attempt fails.
         *
         * @param maxRetries number of retry attempts (must be positive)
         * @return this builder
         */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Builds the {@link RetryableToolCallback}.
         *
         * @return a new {@code RetryableToolCallback} wrapping the delegate
         */
        public RetryableToolCallback build() {
            return new RetryableToolCallback(this);
        }
    }
}
