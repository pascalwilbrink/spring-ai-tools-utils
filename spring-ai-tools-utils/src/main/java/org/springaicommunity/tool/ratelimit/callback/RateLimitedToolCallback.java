package org.springaicommunity.tool.ratelimit.callback;

import com.google.common.util.concurrent.RateLimiter;
import org.springaicommunity.tool.ratelimit.annotation.RateLimitedTool;
import org.springaicommunity.tool.ratelimit.exception.RateLimitExceededException;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Objects;

/**
 * A {@link ToolCallback} decorator that enforces a configurable request rate limit
 * using Guava's {@link com.google.common.util.concurrent.RateLimiter}.
 * <p>
 * If the rate limit is exceeded the call throws
 * {@link org.springaicommunity.tool.ratelimit.exception.RateLimitExceededException}
 * immediately without invoking the delegate.
 * </p>
 * <p>
 * Instances are created through the fluent {@link Builder} returned by
 * {@link #wrap(ToolCallback)}.
 * </p>
 */
public class RateLimitedToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final RateLimiter rateLimiter;

    private RateLimitedToolCallback(Builder builder) {
        this.delegate = builder.delegate;
        this.rateLimiter = buildRateLimiter(builder);
    }

    private static RateLimiter buildRateLimiter(Builder builder) {
        double requestsPerSecond = switch (builder.per) {
            case SECOND -> builder.requests;
            case MINUTE -> builder.requests / 60.0;
            case HOUR   -> builder.requests / 3600.0;
        };

        return RateLimiter.create(requestsPerSecond);
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
        boolean acquired = rateLimiter.tryAcquire();
        if (!acquired) {
            throw new RateLimitExceededException(
                    delegate.getToolDefinition().name(),
                    toolInput
            );
        }

        return toolContext != null
                ? delegate.call(toolInput, toolContext)
                : delegate.call(toolInput);
    }

    /**
     * Begins construction of a {@link RateLimitedToolCallback} wrapping the given delegate.
     *
     * @param delegate the callback to wrap
     * @return a new {@link Builder}
     */
    public static Builder wrap(ToolCallback delegate) {
        return new Builder(delegate);
    }

    /**
     * Fluent builder for {@link RateLimitedToolCallback}.
     */
    public static class Builder {

        private static final int DEFAULT_REQUESTS = 10;
        private static final RateLimitedTool.RateLimit DEFAULT_PER =
                RateLimitedTool.RateLimit.MINUTE;

        private final ToolCallback delegate;
        private int requests = DEFAULT_REQUESTS;
        private RateLimitedTool.RateLimit per = DEFAULT_PER;

        private Builder(ToolCallback delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        }

        /**
         * Sets the maximum number of requests allowed within the time window.
         * Defaults to {@code 10}.
         *
         * @param requests maximum number of calls (must be positive)
         * @return this builder
         */
        public Builder requests(int requests) {
            this.requests = requests;
            return this;
        }

        /**
         * Sets the time window unit for the rate limit.
         * Defaults to {@link RateLimitedTool.RateLimit#MINUTE}.
         *
         * @param per the time window unit
         * @return this builder
         */
        public Builder per(RateLimitedTool.RateLimit per) {
            this.per = per;
            return this;
        }

        /**
         * Builds the {@link RateLimitedToolCallback}.
         *
         * @return the configured {@link RateLimitedToolCallback}
         * @throws IllegalStateException if {@code requests} is not a positive integer
         */
        public RateLimitedToolCallback build() {
            if (requests <= 0) {
                throw new IllegalStateException("requests must be a positive integer");
            }
            return new RateLimitedToolCallback(this);
        }
    }
}