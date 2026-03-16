package org.springaicommunity.tool.guardrails.annotation;

import java.lang.annotation.*;

/**
 * Container annotation that holds multiple {@link InputGuardrail} declarations on a
 * single {@code @Tool} method. Populated automatically by the Java compiler when
 * {@link InputGuardrail} is used more than once.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InputGuardrails {

    /**
     * The individual {@link InputGuardrail} annotations.
     *
     * @return the contained guardrail annotations
     */
    InputGuardrail[] value();
}
