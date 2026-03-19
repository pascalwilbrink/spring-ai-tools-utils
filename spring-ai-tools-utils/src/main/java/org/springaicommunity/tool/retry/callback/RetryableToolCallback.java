package org.springaicommunity.tool.retry.callback;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A {@link ToolCallback} decorator that retries the delegate on exception using
 * Spring Retry's {@link RetryTemplate}.
 *
 * <p>The number of retries is configured via {@link Builder#maxRetries}: the delegate
 * will be called at most {@code maxRetries + 1} times (one initial attempt plus
 * {@code maxRetries} retries) before the last exception is rethrown.</p>
 *
 * <p>Supports fixed and exponential backoff, selective retry on specific exception
 * types, and exclusion of specific exception types from retry.</p>
 */
public class RetryableToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final RetryTemplate retryTemplate;

    private RetryableToolCallback(Builder builder) {
        this.delegate = builder.delegate;
        this.retryTemplate = buildRetryTemplate(builder);
    }

    private static RetryTemplate buildRetryTemplate(Builder builder) {
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();

        // mark retryOn exceptions as retryable
        Arrays.stream(builder.retryOn)
                .forEach(ex -> retryableExceptions.put(ex, true));

        // mark noRetryOn exceptions as non-retryable — takes precedence
        Arrays.stream(builder.noRetryOn)
                .forEach(ex -> retryableExceptions.put(ex, false));

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                builder.maxRetries + 1,  // maxAttempts = initial attempt + retries
                retryableExceptions,
                true                     // traverseCauses — inspect exception hierarchy
        );

        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(retryPolicy);

        if (builder.multiplier > 1.0) {
            ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
            backOff.setInitialInterval(builder.delay);
            backOff.setMultiplier(builder.multiplier);
            template.setBackOffPolicy(backOff);
        } else {
            FixedBackOffPolicy backOff = new FixedBackOffPolicy();
            backOff.setBackOffPeriod(builder.delay);
            template.setBackOffPolicy(backOff);
        }

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
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return retryTemplate.execute(context -> toolContext != null
                ? delegate.call(toolInput, toolContext)
                : delegate.call(toolInput));
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

        private static final int DEFAULT_MAX_RETRIES = 3;
        private static final long DEFAULT_DELAY = 1000L;
        private static final double DEFAULT_MULTIPLIER = 1.0;

        @SuppressWarnings("unchecked")
        private static final Class<? extends Throwable>[] DEFAULT_RETRY_ON =
                new Class[]{ RuntimeException.class };

        @SuppressWarnings("unchecked")
        private static final Class<? extends Throwable>[] DEFAULT_NO_RETRY_ON =
                new Class[0];

        private final ToolCallback delegate;
        private int maxRetries = DEFAULT_MAX_RETRIES;
        private long delay = DEFAULT_DELAY;
        private double multiplier = DEFAULT_MULTIPLIER;
        private Class<? extends Throwable>[] retryOn = DEFAULT_RETRY_ON;
        private Class<? extends Throwable>[] noRetryOn = DEFAULT_NO_RETRY_ON;

        private Builder(ToolCallback delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        }

        /**
         * Sets the maximum number of retries after the initial attempt fails.
         * Defaults to {@code 3}.
         *
         * @param maxRetries number of retry attempts (must be positive)
         * @return this builder
         */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Sets the delay between retry attempts in milliseconds.
         * Defaults to {@code 1000}.
         *
         * @param delay delay in milliseconds
         * @return this builder
         */
        public Builder delay(long delay) {
            this.delay = delay;
            return this;
        }

        /**
         * Sets the multiplier for exponential backoff.
         * Defaults to {@code 1.0} (fixed delay).
         * Set to a value greater than {@code 1.0} to enable exponential backoff.
         *
         * @param multiplier backoff multiplier
         * @return this builder
         */
        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        /**
         * Sets the exception types that should trigger a retry.
         * Defaults to {@code RuntimeException.class}.
         *
         * @param retryOn exception types to retry on
         * @return this builder
         */
        @SafeVarargs
        public final Builder retryOn(Class<? extends Throwable>... retryOn) {
            this.retryOn = retryOn;
            return this;
        }

        /**
         * Sets the exception types that should NOT trigger a retry,
         * even if they match {@link #retryOn}.
         *
         * @param noRetryOn exception types to exclude from retry
         * @return this builder
         */
        @SafeVarargs
        public final Builder noRetryOn(Class<? extends Throwable>... noRetryOn) {
            this.noRetryOn = noRetryOn;
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