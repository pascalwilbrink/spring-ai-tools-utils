package org.springaicommunity.tool.guardrails.annotation;

import java.lang.annotation.*;

/**
 * Container annotation that holds multiple {@link OutputGuardrail} declarations on a
 * single {@code @Tool} method. Populated automatically by the Java compiler when
 * {@link OutputGuardrail} is used more than once.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OutputGuardrails {

    /**
     * The individual {@link OutputGuardrail} annotations.
     *
     * @return the contained guardrail annotations
     */
    OutputGuardrail[] value();
}
