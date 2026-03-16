package org.springaicommunity.tool.confirmation.callback;

import org.springaicommunity.tool.callback.AbstractToolCallbacks;
import org.springaicommunity.tool.confirmation.ConfirmationHandler;
import org.springaicommunity.tool.confirmation.annotation.ConfirmableTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;

/**
 * Resolves the {@link ConfirmableTool} annotation on a tool method and wraps the
 * given {@link ToolCallback} in a {@link ConfirmableToolCallback} when the annotation
 * is present.
 */
public class ConfirmableToolCallbacks extends AbstractToolCallbacks {

    /**
     * Creates an instance backed by the given application context.
     *
     * @param applicationContext the context used to resolve {@link ConfirmationHandler} beans
     */
    public ConfirmableToolCallbacks(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    /**
     * Wraps {@code cb} with confirmation logic derived from the {@link ConfirmableTool}
     * annotation on {@code method}. Returns {@code cb} unchanged when the annotation
     * is absent.
     *
     * @param cb     the callback to wrap
     * @param method the tool method whose annotations are inspected
     * @return the wrapped callback, or the original callback if no annotation is present
     */
    public ToolCallback wrap(ToolCallback cb, Method method) {
        ConfirmableTool annotation = method.getAnnotation(ConfirmableTool.class);
        if (annotation == null) return cb;

        return ConfirmableToolCallback.wrap(cb)
            .confirmation(resolve(annotation.handler(), ConfirmationHandler.class))
            .reason(annotation.reason())
            .build();
    }

}