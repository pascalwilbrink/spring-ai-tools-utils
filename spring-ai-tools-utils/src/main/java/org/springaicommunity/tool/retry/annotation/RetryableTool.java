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

    /**
     * Delay between retry attempts in milliseconds. Defaults to {@code 1000}.
     */
    long delay() default 1000;

    /**
     * Multiplier for exponential backoff. Defaults to {@code 1.0} (fixed delay).
     * Set to a value greater than 1.0 to enable exponential backoff.
     * For example, 2.0 doubles the delay after each attempt.
     */
    double multiplier() default 1.0;

    /**
     * Exception types that should trigger a retry.
     * Defaults to {@code RuntimeException.class} — all runtime exceptions.
     */
    Class<? extends Throwable>[] retryOn() default { RuntimeException.class };

    /**
     * Exception types that should NOT trigger a retry, even if they match {@link #retryOn()}.
     * Useful for skipping retries on specific known non-transient exceptions.
     */
    Class<? extends Throwable>[] noRetryOn() default {};

}
