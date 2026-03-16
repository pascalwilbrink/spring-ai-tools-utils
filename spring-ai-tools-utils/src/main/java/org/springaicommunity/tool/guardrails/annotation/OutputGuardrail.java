package org.springaicommunity.tool.guardrails.annotation;

import org.springaicommunity.tool.guardrails.output.ToolOutputGuardrail;

import java.lang.annotation.*;

/**
 * Declares a {@link ToolOutputGuardrail} to be applied to a {@code @Tool} method
 * after the tool has executed. Repeatable — use multiple annotations or the
 * container {@link OutputGuardrails} to attach several guardrails.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(OutputGuardrails.class)
public @interface OutputGuardrail {

    /**
     * The {@link ToolOutputGuardrail} implementation to apply.
     * The class is resolved from the application context if available,
     * otherwise instantiated via its no-arg constructor.
     *
     * @return the guardrail implementation class
     */
    Class<? extends ToolOutputGuardrail> value();
}
