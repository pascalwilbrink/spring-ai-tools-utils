package org.springaicommunity.tool.logging.callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.tool.confirmation.exception.ToolRejectionException;
import org.springaicommunity.tool.guardrails.exception.GuardrailViolationException;
import org.springaicommunity.tool.ratelimit.exception.RateLimitExceededException;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Objects;

/**
 * A {@link ToolCallback} decorator that logs tool call lifecycle events at DEBUG and
 * WARN/ERROR level using SLF4J.
 * <p>
 * Records the start, successful completion, and any exception (guardrail violation,
 * rejection, rate limit, or unexpected failure) of each tool invocation, including
 * the tool name and elapsed duration in milliseconds.
 * </p>
 * <p>
 * Instances are created through the fluent {@link Builder} returned by
 * {@link #wrap(ToolCallback)}.
 * </p>
 */
public class LoggingToolCallback implements ToolCallback {

    private static final Logger logger =
            LoggerFactory.getLogger(LoggingToolCallback.class);

    private final ToolCallback delegate;

    private LoggingToolCallback(Builder builder) {
        this.delegate = builder.delegate;
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
        String toolName = delegate.getToolDefinition().name();
        long start = System.currentTimeMillis();

        logger.debug("Tool call started  | tool={} input={}", toolName, toolInput);

        try {
            String output = toolContext != null
                    ? delegate.call(toolInput, toolContext)
                    : delegate.call(toolInput);

            long duration = System.currentTimeMillis() - start;

            logger.debug("Tool call success  | tool={} duration={}ms output={}",
                    toolName, duration, output);

            return output;

        } catch (GuardrailViolationException e) {
            long duration = System.currentTimeMillis() - start;
            logger.warn("Tool call blocked  | tool={} duration={}ms reason={}",
                    toolName, duration, e.getMessage());
            throw e;

        } catch (ToolRejectionException e) {
            long duration = System.currentTimeMillis() - start;
            logger.warn("Tool call rejected | tool={} duration={}ms reason={}",
                    toolName, duration, e.getMessage());
            throw e;

        } catch (RateLimitExceededException e) {
            long duration = System.currentTimeMillis() - start;
            logger.warn("Tool call limited  | tool={} duration={}ms reason={}",
                    toolName, duration, e.getMessage());
            throw e;

        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - start;
            logger.error("Tool call failed   | tool={} duration={}ms error={}",
                    toolName, duration, t.getMessage(), t);
            throw t;
        }
    }

    /**
     * Begins construction of a {@link LoggingToolCallback} wrapping the given delegate.
     *
     * @param delegate the callback to wrap
     * @return a new {@link Builder}
     */
    public static Builder wrap(ToolCallback delegate) {
        return new Builder(delegate);
    }

    /**
     * Fluent builder for {@link LoggingToolCallback}.
     */
    public static class Builder {

        private final ToolCallback delegate;

        private Builder(ToolCallback delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        }

        /**
         * Builds the {@link LoggingToolCallback}.
         *
         * @return the configured {@link LoggingToolCallback}
         */
        public LoggingToolCallback build() {
            return new LoggingToolCallback(this);
        }
    }
}