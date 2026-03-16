package org.springaicommunity.tool.confirmation.annotation;

import org.springaicommunity.tool.confirmation.ConfirmationHandler;

import java.lang.annotation.*;

/**
 * Marks a {@code @Tool} method as requiring explicit human confirmation before
 * execution. The nominated {@link ConfirmationHandler} is invoked at runtime to
 * obtain approval.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfirmableTool {

    /**
     * The {@link ConfirmationHandler} implementation to use for this tool.
     * The class is resolved from the application context if available,
     * otherwise instantiated via its no-arg constructor.
     *
     * @return the confirmation handler class
     */
    Class<? extends ConfirmationHandler> handler();

    /**
     * Human-readable explanation presented to the user when requesting confirmation.
     *
     * @return the reason string shown during the confirmation prompt
     */
    String reason() default "This action requires your approval.";
}
