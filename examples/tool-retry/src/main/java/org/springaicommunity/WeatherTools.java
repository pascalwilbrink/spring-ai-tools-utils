package org.springaicommunity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.tool.retry.annotation.RetryableTool;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Example tools representing a flaky weather service.
 *
 * <p>{@code getWeather} simulates a transient upstream failure on the first two
 * attempts before succeeding, demonstrating that {@code @RetryableTool} transparently
 * retries the call without the AI ever seeing the error.
 */
@Component
public class WeatherTools {

    private static final Logger log = LoggerFactory.getLogger(WeatherTools.class);

    private final AtomicInteger attempts = new AtomicInteger(0);

    @Tool(description = "Get the current weather for a given city")
    @RetryableTool(maxRetries = 3)
    public String getWeather(String city) {
        int attempt = attempts.incrementAndGet();
        if (attempt < 3) {
            log.warn("Weather service unavailable (attempt {}), retrying...", attempt);
            throw new RuntimeException("Weather service temporarily unavailable");
        }
        log.info("Weather service responded on attempt {}", attempt);
        attempts.set(0); // reset for the next call
        return "The weather in %s is 22°C and sunny.".formatted(city);
    }

    @Tool(description = "Get a 5-day forecast for a given city")
    public String getForecast(String city) {
        return """
            5-day forecast for %s:
            - Day 1: 22°C, Sunny
            - Day 2: 19°C, Partly cloudy
            - Day 3: 17°C, Rainy
            - Day 4: 20°C, Cloudy
            - Day 5: 23°C, Sunny
            """.formatted(city);
    }
}
