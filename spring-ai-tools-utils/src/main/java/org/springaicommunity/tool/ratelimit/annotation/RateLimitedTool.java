package org.springaicommunity.tool.ratelimit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code @Tool}-annotated method as rate-limited.
 * <p>
 * When the configured limit is exceeded the tool throws
 * {@link org.springaicommunity.tool.ratelimit.exception.RateLimitExceededException}
 * instead of invoking the delegate.
 * </p>
 *
 * <p>Example usage:
 * <pre>{@code
 * @Tool(description = "Get the current exchange rate")
 * @RateLimitedTool(requests = 60, per = RateLimitedTool.RateLimit.MINUTE)
 * public String getExchangeRate(String currency) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimitedTool {

    /**
     * Maximum number of calls allowed within the time window.
     * Must be a positive integer. Defaults to {@code 10}.
     */
    int requests() default 10;

    /**
     * Time window unit. Defaults to {@code MINUTE}.
     */
    RateLimit per() default RateLimit.MINUTE;

    /**
     * Time window for the rate limit.
     */
    enum RateLimit {
        SECOND, MINUTE, HOUR
    }

}
