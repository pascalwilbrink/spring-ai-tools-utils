package org.springaicommunity.tool.callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.tool.confirmation.callback.ConfirmableToolCallbacks;
import org.springaicommunity.tool.fallback.callback.FallbackToolCallbacks;
import org.springaicommunity.tool.guardrails.callback.GuardedToolCallbacks;
import org.springaicommunity.tool.retry.callback.RetryableToolCallbacks;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * Factory that creates {@link ToolCallback} arrays from a tool object, applying
 * guardrail, confirmation, and fallback decorators in that order.
 */
public class ToolCallbacksFactory {

    private static final Logger log = LoggerFactory.getLogger(ToolCallbacksFactory.class);

    private final GuardedToolCallbacks guardedToolCallbacks;
    private final ConfirmableToolCallbacks confirmableToolCallbacks;
    private final FallbackToolCallbacks fallbackToolCallbacks;
    private final RetryableToolCallbacks retryableToolCallbacks;

    /**
     * Constructs the factory with the decorator layers that will be applied to
     * every tool callback produced by {@link #from(Object)}.
     *
     * @param guardedToolCallbacks      applies input/output guardrails
     * @param confirmableToolCallbacks  applies human-confirmation logic
     * @param fallbackToolCallbacks     applies fallback strategies on failure
     * @param retryableToolCallbacks    applies retry logic on exception
     */
    public ToolCallbacksFactory(
        GuardedToolCallbacks guardedToolCallbacks,
        ConfirmableToolCallbacks confirmableToolCallbacks,
        FallbackToolCallbacks fallbackToolCallbacks,
        RetryableToolCallbacks retryableToolCallbacks
    ) {
        this.guardedToolCallbacks = guardedToolCallbacks;
        this.confirmableToolCallbacks = confirmableToolCallbacks;
        this.fallbackToolCallbacks = fallbackToolCallbacks;
        this.retryableToolCallbacks = retryableToolCallbacks;
    }

    /**
     * Builds decorated {@link ToolCallback} instances for all {@code @Tool}-annotated
     * methods found on the given tool object.
     *
     * @param toolObject the bean that declares the tool methods
     * @return array of callbacks with guardrail, confirmation, and fallback layers applied
     */
    public ToolCallback[] from(Object toolObject) {
        return Arrays.stream(ToolCallbacks.from(toolObject))
            .map(cb -> applyDecorators(cb, toolObject))
            .toArray(ToolCallback[]::new);
    }

    private ToolCallback applyDecorators(ToolCallback cb, Object toolObject) {
        Method method = findToolMethod(
            toolObject.getClass(),
            cb.getToolDefinition().name()
        );

        if (method == null) return cb;

        cb = guardedToolCallbacks.wrap(cb, method);
        cb = confirmableToolCallbacks.wrap(cb, method);
        cb = retryableToolCallbacks.wrap(cb, method, toolObject);
        cb = fallbackToolCallbacks.wrap(cb, method, toolObject);

        return cb;
    }

    private Method findToolMethod(Class<?> clazz, String toolName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            Optional<Method> found = Arrays.stream(current.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Tool.class))
                .filter(m -> {
                    Tool tool = m.getAnnotation(Tool.class);
                    String name = tool.name().isBlank() ? m.getName() : tool.name();
                    return name.equals(toolName);
                })
                .findFirst();
            if (found.isPresent()) return found.get();
            current = current.getSuperclass();
        }
        log.warn("No @Tool method found for tool name '{}' on {} or any superclass. " +
            "Guardrail and confirmation annotations will be ignored.",
            toolName, clazz.getName());
        return null;
    }
}