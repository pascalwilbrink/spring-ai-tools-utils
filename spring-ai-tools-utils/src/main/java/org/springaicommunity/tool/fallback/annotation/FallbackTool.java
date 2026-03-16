package org.springaicommunity.tool.fallback.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code @Tool} method with a recovery strategy to use when the method throws
 * an exception. Exactly one of {@link #method()} or {@link #message()} must be specified.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FallbackTool {

    /**
     * Name of the fallback method on the same class to invoke when the tool fails.
     * The method must have the same parameters as the tool method,
     * optionally with a Throwable as a final extra parameter.
     *
     * <p>Example:</p>
     * <pre>{@code
     * @Tool(description = "Execute a SQL query")
     * @FallbackTool(method = "queryFailed")
     * String executeQuery(String sql) { ... }
     *
     * String queryFailed(String sql, Throwable cause) { ... }
     * }</pre>
     *
     * @return the name of the fallback method, or empty string if not used
     */
    String method() default "";

    /**
     * Static message to return to the model when the tool fails.
     * Used when no fallback method is needed.
     *
     * <p>Example:</p>
     * <pre>{@code
     * @Tool(description = "Get weather")
     * @FallbackTool(message = "Weather service is currently unavailable.")
     * String getWeather(String city) { ... }
     * }</pre>
     *
     * @return the static fallback message, or empty string if not used
     */
    String message() default "";
}
