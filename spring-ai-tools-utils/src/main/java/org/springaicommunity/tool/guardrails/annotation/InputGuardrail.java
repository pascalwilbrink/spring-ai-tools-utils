package org.springaicommunity.tool.guardrails.annotation;

import org.springaicommunity.tool.guardrails.input.ToolInputGuardrail;

import java.lang.annotation.*;

/**
 * Declares a {@link ToolInputGuardrail} to be applied to a {@code @Tool} method
 * before the tool is executed. Repeatable — use multiple annotations or the
 * container {@link InputGuardrails} to attach several guardrails.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(InputGuardrails.class)
public @interface InputGuardrail {

    /**
     * The {@link ToolInputGuardrail} implementation to apply.
     * The class is resolved from the application context if available,
     * otherwise instantiated via its no-arg constructor.
     *
     * @return the guardrail implementation class
     */
    Class<? extends ToolInputGuardrail> value();
}