package org.springaicommunity.tool.ratelimit.callback;

import org.springaicommunity.tool.callback.AbstractToolCallbacks;
import org.springaicommunity.tool.ratelimit.annotation.RateLimitedTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Resolves the {@link RateLimitedTool} annotation on a tool method and wraps the given
 * {@link ToolCallback} in a {@link RateLimitedToolCallback} when the annotation is present.
 */
@Component
public class RateLimitedToolCallbacks extends AbstractToolCallbacks {

    /**
     * Creates an instance backed by the given application context.
     *
     * @param applicationContext the context used to resolve strategy beans
     */
    public RateLimitedToolCallbacks(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    /**
     * Wraps {@code cb} with rate-limiting derived from the {@link RateLimitedTool}
     * annotation on {@code method}. Returns {@code cb} unchanged when the annotation
     * is absent.
     *
     * @param cb     the callback to wrap
     * @param method the tool method whose annotations are inspected
     * @return the wrapped callback, or the original callback if no annotation is present
     * @throws IllegalStateException if {@link RateLimitedTool#requests()} is not positive
     */
    public ToolCallback wrap(ToolCallback cb, Method method) {
        RateLimitedTool annotation = method.getAnnotation(RateLimitedTool.class);
        if (annotation == null) return cb;

        validate(annotation, method);

        return RateLimitedToolCallback.wrap(cb)
                .requests(annotation.requests())
                .per(annotation.per())
                .build();
    }

    private void validate(RateLimitedTool annotation, Method method) {
        if (annotation.requests() <= 0) {
            throw new IllegalStateException(
                    "@RateLimitedTool on '%s' must specify a positive requests() value"
                            .formatted(method.getName())
            );
        }
    }
}