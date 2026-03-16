package org.springaicommunity.tool.guardrails.callback;

import org.springaicommunity.tool.callback.AbstractToolCallbacks;
import org.springaicommunity.tool.guardrails.annotation.InputGuardrail;
import org.springaicommunity.tool.guardrails.annotation.InputGuardrails;
import org.springaicommunity.tool.guardrails.annotation.OutputGuardrail;
import org.springaicommunity.tool.guardrails.annotation.OutputGuardrails;
import org.springaicommunity.tool.guardrails.input.ToolInputGuardrail;
import org.springaicommunity.tool.guardrails.output.ToolOutputGuardrail;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Resolves {@link InputGuardrail} and {@link OutputGuardrail} annotations declared on
 * a tool method and wraps the given {@link ToolCallback} in a
 * {@link GuardedToolCallback} if any guardrails are present.
 */
public class GuardedToolCallbacks extends AbstractToolCallbacks {

    /**
     * Creates an instance backed by the given application context.
     *
     * @param applicationContext the context used to resolve guardrail beans
     */
    public GuardedToolCallbacks(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    /**
     * Wraps {@code cb} with guardrails derived from annotations on {@code method}.
     * Returns {@code cb} unchanged when no guardrail annotations are present.
     *
     * @param cb     the callback to wrap
     * @param method the tool method whose annotations are inspected
     * @return the wrapped callback, or the original callback if no annotations are present
     */
    public ToolCallback wrap(ToolCallback cb, Method method) {
        List<ToolInputGuardrail> inputGuardrails = resolveInputGuardrails(method);
        List<ToolOutputGuardrail> outputGuardrails = resolveOutputGuardrails(method);

        if (inputGuardrails.isEmpty() && outputGuardrails.isEmpty()) return cb;

        GuardedToolCallback.Builder builder = GuardedToolCallback.wrap(cb);
        inputGuardrails.forEach(builder::inputGuardrail);
        outputGuardrails.forEach(builder::outputGuardrail);

        return builder.build();
    }

    private List<ToolInputGuardrail> resolveInputGuardrails(Method method) {
        InputGuardrails container = method.getAnnotation(InputGuardrails.class);
        InputGuardrail single = method.getAnnotation(InputGuardrail.class);

        List<InputGuardrail> annotations = container != null
            ? Arrays.asList(container.value())
            : single != null ? List.of(single) : List.of();

        return annotations.stream()
            .map(a -> resolve(a.value(), ToolInputGuardrail.class))
            .toList();
    }

    private List<ToolOutputGuardrail> resolveOutputGuardrails(Method method) {
        OutputGuardrails container = method.getAnnotation(OutputGuardrails.class);
        OutputGuardrail single = method.getAnnotation(OutputGuardrail.class);

        List<OutputGuardrail> annotations = container != null
            ? Arrays.asList(container.value())
            : single != null ? List.of(single) : List.of();

        return annotations.stream()
            .map(a -> resolve(a.value(), ToolOutputGuardrail.class))
            .toList();
    }

}