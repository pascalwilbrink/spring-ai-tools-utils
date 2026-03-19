package org.springaicommunity.tool.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import org.springaicommunity.tool.callback.ToolCallbacksFactory;
import org.springaicommunity.tool.confirmation.callback.ConfirmableToolCallbacks;
import org.springaicommunity.tool.confirmation.properties.ConfirmationProperties;
import org.springaicommunity.tool.confirmation.store.ConfirmationStore;
import org.springaicommunity.tool.confirmation.store.InMemoryConfirmationStore;
import org.springaicommunity.tool.fallback.callback.FallbackToolCallbacks;
import org.springaicommunity.tool.guardrails.callback.GuardedToolCallbacks;
import org.springaicommunity.tool.ratelimit.callback.RateLimitedToolCallbacks;
import org.springaicommunity.tool.retry.callback.RetryableToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration that registers the core infrastructure beans for
 * tool guardrails, confirmation handling, fallback strategies, and the
 * {@link ToolCallbacksFactory}.
 * <p>
 * All beans are conditional on absence of a user-provided bean of the same type,
 * allowing full customisation.
 * </p>
 */
@AutoConfiguration
@ConditionalOnClass(ToolCallback.class)
@EnableConfigurationProperties(ConfirmationProperties.class)
public class ToolsAutoConfiguration {

    /**
     * Provides the default in-memory {@link ConfirmationStore} when no custom
     * implementation is present in the application context.
     */
    @Bean
    @ConditionalOnMissingBean
    public ConfirmationStore confirmationStore() {
        return new InMemoryConfirmationStore();
    }

    /**
     * Provides the default {@link GuardedToolCallbacks} bean for applying input and
     * output guardrails to tool callbacks.
     */
    @Bean
    @ConditionalOnMissingBean
    public GuardedToolCallbacks guardedToolCallbacks(ApplicationContext ctx) {
        return new GuardedToolCallbacks(ctx);
    }

    /**
     * Provides the default {@link ConfirmableToolCallbacks} bean for wrapping tool
     * callbacks with human-confirmation logic.
     */
    @Bean
    @ConditionalOnMissingBean
    public ConfirmableToolCallbacks confirmableToolCallbacks(ApplicationContext ctx) {
        return new ConfirmableToolCallbacks(ctx);
    }

    /**
     * Provides the default {@link FallbackToolCallbacks} bean for attaching fallback
     * strategies to tool callbacks.
     */
    @Bean
    @ConditionalOnMissingBean
    public FallbackToolCallbacks fallbackToolCallbacks(ApplicationContext ctx) {
        return new FallbackToolCallbacks(ctx);
    }

    /**
     * Provides the default {@link RateLimitedToolCallbacks} bean for attaching fallback
     * strategies to tool callbacks.
     */
    @Bean
    @ConditionalOnMissingBean
    public RateLimitedToolCallbacks rateLimitedToolCallbacks(ApplicationContext ctx) {
        return new RateLimitedToolCallbacks(ctx);
    }

    /**
     * Provides the default {@link RetryableToolCallbacks} bean for retrying failed
     * tool calls when annotated with {@code @RetryableTool}.
     */
    @Bean
    @ConditionalOnMissingBean
    public RetryableToolCallbacks retryableToolCallbacks(ApplicationContext ctx) {
        return new RetryableToolCallbacks(ctx);
    }

    /**
     * Provides the default {@link ToolCallbacksFactory} bean that composes all
     * decorator layers into a single factory.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    public ToolCallbacksFactory toolCallbacksFactory(
            GuardedToolCallbacks guardedToolCallbacks,
            RateLimitedToolCallbacks rateLimitedToolCallbacks,
            RetryableToolCallbacks retryableToolCallbacks,
            ConfirmableToolCallbacks confirmableToolCallbacks,
            FallbackToolCallbacks fallbackToolCallbacks,
            MeterRegistry meterRegistry) {
        return new ToolCallbacksFactory(
                guardedToolCallbacks,
                confirmableToolCallbacks,
                fallbackToolCallbacks,
                retryableToolCallbacks,
                rateLimitedToolCallbacks,
                meterRegistry
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnMissingClass("io.micrometer.core.instrument.MeterRegistry")
    public ToolCallbacksFactory toolCallbacksFactoryWithoutMetrics(
            GuardedToolCallbacks guardedToolCallbacks,
            RateLimitedToolCallbacks rateLimitedToolCallbacks,
            RetryableToolCallbacks retryableToolCallbacks,
            ConfirmableToolCallbacks confirmableToolCallbacks,
            FallbackToolCallbacks fallbackToolCallbacks) {
        return new ToolCallbacksFactory(
                guardedToolCallbacks,
                confirmableToolCallbacks,
                fallbackToolCallbacks,
                retryableToolCallbacks,
                rateLimitedToolCallbacks,
                null
        );
    }
}