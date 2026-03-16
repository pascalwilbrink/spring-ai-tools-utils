package org.springaicommunity.tool.fallback.strategy;

import java.lang.reflect.Method;

/**
 * Strategy that produces an alternative response when a tool invocation fails.
 * <p>
 * Implementations receive the original tool input and the throwable that caused the
 * failure, and must return the string that will be returned to the model in place of
 * the real tool output.
 * </p>
 */
@FunctionalInterface
public interface FallbackToolStrategy {

    /**
     * Produces a fallback response for a failed tool invocation.
     *
     * @param toolInput the raw tool input that was passed when the failure occurred
     * @param cause     the exception thrown by the tool
     * @return the fallback string to return to the caller
     */
    String fallback(String toolInput, Throwable cause);

    /**
     * Creates a strategy that always returns the given static message, ignoring the
     * input and cause.
     *
     * @param message the fixed response to return on failure
     * @return a strategy that always returns {@code message}
     */
    static FallbackToolStrategy withMessage(String message) {
        return (input, cause) -> message;
    }

    /**
     * Creates a strategy that invokes a specific method on {@code toolObject} to
     * compute the fallback response. The method signature is matched flexibly:
     * no parameters, just the cause, just the input, or both input and cause.
     *
     * @param toolObject     the bean on which the fallback method will be invoked
     * @param fallbackMethod the reflective method to call on failure
     * @return a strategy that delegates to {@code fallbackMethod} on failure
     * @throws IllegalStateException if the fallback method itself throws
     */
    static FallbackToolStrategy withMethod(Object toolObject, Method fallbackMethod) {
        return (input, cause) -> {
            try {
                fallbackMethod.setAccessible(true);
                Class<?>[] params = fallbackMethod.getParameterTypes();

                Object result;

                if (params.length == 0) {
                    result = fallbackMethod.invoke(toolObject);
                } else if (
                    params.length == 1
                    && Throwable.class.isAssignableFrom(params[0])
                ) {
                    result = fallbackMethod.invoke(toolObject, cause);
                } else if (params.length == 1) {
                    result = fallbackMethod.invoke(toolObject, input);
                } else {
                    result = fallbackMethod.invoke(toolObject, input, cause);
                }

                return result != null ? result.toString() : "";

            } catch (Exception ex) {
                throw new IllegalStateException(
                    "Fallback method '%s' threw an exception".formatted(fallbackMethod.getName()), ex
                );
            }
        };
    }
}
