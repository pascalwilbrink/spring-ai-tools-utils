package org.springaicommunity.tool.fallback.callback;

import org.springaicommunity.tool.callback.AbstractToolCallbacks;
import org.springaicommunity.tool.fallback.annotation.FallbackTool;
import org.springaicommunity.tool.fallback.strategy.FallbackToolStrategy;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Resolves the {@link FallbackTool} annotation on a tool method and wraps the given
 * {@link ToolCallback} in a {@link FallbackToolCallback} backed by the appropriate
 * {@link FallbackToolStrategy}.
 */
@Component
public class FallbackToolCallbacks extends AbstractToolCallbacks {

    /**
     * Creates an instance backed by the given application context.
     *
     * @param applicationContext the context used to resolve strategy beans
     */
    public FallbackToolCallbacks(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    /**
     * Wraps {@code cb} with a fallback strategy derived from the {@link FallbackTool}
     * annotation on {@code method}. Returns {@code cb} unchanged when the annotation
     * is absent.
     *
     * @param cb         the callback to wrap
     * @param method     the tool method whose annotations are inspected
     * @param toolObject the bean instance that owns the tool method, used to invoke
     *                   named fallback methods via reflection
     * @return the wrapped callback, or the original callback if no annotation is present
     * @throws IllegalStateException if the annotation is present but specifies neither
     *                               or both of {@code method()} and {@code message()}
     */
    public ToolCallback wrap(ToolCallback cb, Method method, Object toolObject) {
        FallbackTool annotation = method.getAnnotation(FallbackTool.class);
        if (annotation == null) return cb;

        validate(annotation, method);

        FallbackToolStrategy strategy = annotation.method().isBlank()
            ? FallbackToolStrategy.withMessage(annotation.message())
            : FallbackToolStrategy.withMethod(toolObject, findFallbackMethod(
            toolObject.getClass(),
            annotation.method(),
            method.getParameterTypes()
        ));

        return FallbackToolCallback.wrap(cb)
                .fallback(strategy)
                .build();
    }

    private void validate(FallbackTool annotation, Method method) {
        boolean hasMethod = !annotation.method().isBlank();
        boolean hasMessage = !annotation.message().isBlank();

        if (!hasMethod && !hasMessage) {
            throw new IllegalStateException(
                    "@FallbackTool on '%s' must specify either method() or message()"
                            .formatted(method.getName())
            );
        }

        if (hasMethod && hasMessage) {
            throw new IllegalStateException(
                    "@FallbackTool on '%s' cannot specify both method() and message()"
                            .formatted(method.getName())
            );
        }
    }

    private Method findFallbackMethod(Class<?> clazz,
                                      String name,
                                      Class<?>[] toolParams) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.getName().equals(name))
                .filter(m -> matchesSignature(m, toolParams))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No fallback method '%s' found on %s matching expected signature"
                                .formatted(name, clazz.getName())
                ));
    }

    private boolean matchesSignature(Method method, Class<?>[] toolParams) {
        Class<?>[] params = method.getParameterTypes();

        // () — no params, no cause
        if (toolParams.length == 0 && params.length == 0) {
            return true;
        }

        // (Throwable) — no tool params, just cause
        if (toolParams.length == 0 && params.length == 1) {
            return Throwable.class.isAssignableFrom(params[0]);
        }

        // (sameParams) — without cause
        if (params.length == toolParams.length) {
            return Arrays.equals(params, toolParams);
        }

        // (sameParams, Throwable) — with cause
        if (params.length == toolParams.length + 1) {
            return Arrays.equals(Arrays.copyOf(params, toolParams.length), toolParams)
                    && Throwable.class.isAssignableFrom(params[params.length - 1]);
        }

        return false;
    }
}
