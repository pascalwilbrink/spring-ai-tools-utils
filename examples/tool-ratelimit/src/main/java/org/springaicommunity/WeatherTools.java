package org.springaicommunity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.tool.ratelimit.annotation.RateLimitedTool;
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


    @Tool(description = "Get the current weather for a given city")
    @RateLimitedTool(per = RateLimitedTool.RateLimit.MINUTE, requests = 3)
    public String getWeather(String city) {
        return "Weather unavailable for %s".formatted(city);
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
