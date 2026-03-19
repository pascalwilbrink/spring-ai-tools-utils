package org.springaicommunity.tool.retry.callback;

import org.springaicommunity.tool.callback.AbstractToolCallbacks;
import org.springaicommunity.tool.retry.annotation.RetryableTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Decorator that wraps a {@link ToolCallback} with retry logic when the corresponding
 * method is annotated with {@link RetryableTool}.
 *
 * <p>If the annotation is absent the callback is returned unchanged. If present,
 * the callback is wrapped in a {@link RetryableToolCallback} configured with the
 * {@link RetryableTool#maxRetries()} value from the annotation.
 */
@Component
public class RetryableToolCallbacks extends AbstractToolCallbacks {

    /**
     * Creates the decorator with access to the application context for resolving
     * handler beans.
     *
     * @param applicationContext the Spring application context
     */
    public RetryableToolCallbacks(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    /**
     * Wraps {@code cb} with retry logic if {@code method} carries {@link RetryableTool}.
     *
     * @param cb         the callback to wrap
     * @param method     the tool method, inspected for the {@link RetryableTool} annotation
     * @return the original callback if the annotation is absent, otherwise a
     *         {@link RetryableToolCallback} with the configured retry policy
     * @throws IllegalStateException if {@link RetryableTool#maxRetries()} is not positive
     */
    public ToolCallback wrap(ToolCallback cb, Method method) {
        RetryableTool annotation = method.getAnnotation(RetryableTool.class);
        if (annotation == null) return cb;

        validate(annotation, method);

        return RetryableToolCallback.wrap(cb)
            .maxRetries(annotation.maxRetries())
            .delay(annotation.delay())
            .multiplier(annotation.multiplier())
            .retryOn(annotation.retryOn())
            .noRetryOn(annotation.noRetryOn())
            .build();
    }

    private void validate(RetryableTool annotation, Method method) {
        boolean hasMaxRetries = annotation.maxRetries() > 0;

        if (!hasMaxRetries) {
            throw new IllegalStateException(
                "@RetryableTool on '%s' must specify a positive maxRetries()"
                    .formatted(method.getName())
            );
        }

    }

}
