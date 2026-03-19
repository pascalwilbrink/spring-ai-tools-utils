package org.springaicommunity.tool.metrics.callback;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springaicommunity.tool.confirmation.exception.ToolRejectionException;
import org.springaicommunity.tool.guardrails.exception.GuardrailViolationException;
import org.springaicommunity.tool.ratelimit.exception.RateLimitExceededException;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Objects;

/**
 * A {@link ToolCallback} decorator that records Micrometer metrics for each tool
 * invocation.
 * <p>
 * Publishes two metrics for every call:
 * <ul>
 *   <li>{@value #METRIC_CALL_DURATION} — a {@link io.micrometer.core.instrument.Timer}
 *       tagged with the tool name and outcome</li>
 *   <li>{@value #METRIC_CALL_COUNT} — a {@link io.micrometer.core.instrument.Counter}
 *       tagged with the tool name and outcome</li>
 * </ul>
 * Possible outcome tag values: {@code success}, {@code blocked}, {@code rejected},
 * {@code rate_limited}, {@code failure}.
 * </p>
 * <p>
 * Instances are created through the fluent {@link Builder} returned by
 * {@link #wrap(ToolCallback)}.
 * </p>
 */
public class MetricsToolCallback implements ToolCallback {

    private static final String METRIC_CALL_COUNT    = "tools.call.count";
    private static final String METRIC_CALL_DURATION = "tools.call.duration";

    private static final String TAG_TOOL    = "tool";
    private static final String TAG_OUTCOME = "outcome";

    private static final String OUTCOME_SUCCESS     = "success";
    private static final String OUTCOME_BLOCKED     = "blocked";
    private static final String OUTCOME_REJECTED    = "rejected";
    private static final String OUTCOME_RATE_LIMITED = "rate_limited";
    private static final String OUTCOME_FAILURE     = "failure";

    private final ToolCallback delegate;
    private final MeterRegistry meterRegistry;

    private MetricsToolCallback(Builder builder) {
        this.delegate = builder.delegate;
        this.meterRegistry = builder.meterRegistry;
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
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            String output = toolContext != null
                    ? delegate.call(toolInput, toolContext)
                    : delegate.call(toolInput);

            record(sample, toolName, OUTCOME_SUCCESS);
            return output;

        } catch (GuardrailViolationException e) {
            record(sample, toolName, OUTCOME_BLOCKED);
            throw e;

        } catch (ToolRejectionException e) {
            record(sample, toolName, OUTCOME_REJECTED);
            throw e;

        } catch (RateLimitExceededException e) {
            record(sample, toolName, OUTCOME_RATE_LIMITED);
            throw e;

        } catch (Throwable t) {
            record(sample, toolName, OUTCOME_FAILURE);
            throw t;
        }
    }

    private void record(Timer.Sample sample, String toolName, String outcome) {
        // duration
        sample.stop(Timer.builder(METRIC_CALL_DURATION)
                .tag(TAG_TOOL, toolName)
                .tag(TAG_OUTCOME, outcome)
                .register(meterRegistry));

        // count
        Counter.builder(METRIC_CALL_COUNT)
                .tag(TAG_TOOL, toolName)
                .tag(TAG_OUTCOME, outcome)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Begins construction of a {@link MetricsToolCallback} wrapping the given delegate.
     *
     * @param delegate the callback to wrap
     * @return a new {@link Builder}
     */
    public static Builder wrap(ToolCallback delegate) {
        return new Builder(delegate);
    }

    /**
     * Fluent builder for {@link MetricsToolCallback}.
     */
    public static class Builder {

        private final ToolCallback delegate;
        private MeterRegistry meterRegistry;

        private Builder(ToolCallback delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        }

        /**
         * Sets the {@link MeterRegistry} to use for recording metrics.
         *
         * @param meterRegistry the registry to record into
         * @return this builder
         */
        public Builder meterRegistry(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            return this;
        }

        /**
         * Builds the {@link MetricsToolCallback}.
         *
         * @return the configured {@link MetricsToolCallback}
         * @throws NullPointerException if no {@link MeterRegistry} has been set
         */
        public MetricsToolCallback build() {
            Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
            return new MetricsToolCallback(this);
        }
    }
}