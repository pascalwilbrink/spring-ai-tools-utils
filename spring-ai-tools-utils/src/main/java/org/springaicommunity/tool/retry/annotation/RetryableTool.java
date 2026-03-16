package org.springaicommunity.tool.retry.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code @Tool}-annotated method as retryable when it throws an exception.
 *
 * <p>The decorated tool will be retried up to {@link #maxRetries} additional times
 * using a fixed 1-second back-off between attempts. If all attempts fail the last
 * exception is rethrown.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Tool(description = "Fetch data from a flaky external API")
 * @RetryableTool(maxRetries = 3)
 * public String fetchData(String query) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RetryableTool {

    /**
     * Maximum number of retry attempts after the initial call fails.
     * Must be a positive integer. Defaults to {@code 3}.
     */
    int maxRetries() default 3;

}
