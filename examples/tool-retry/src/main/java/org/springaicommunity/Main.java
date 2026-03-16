package org.springaicommunity;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

/**
 * Entry point for the tool-retry example.
 *
 * <p>Demonstrates {@code @RetryableTool}: the weather tool simulates a transient
 * failure on the first two attempts and succeeds on the third. Spring Retry handles
 * the retries transparently — the AI model never sees the error.
 *
 * <p>Watch the logs for "retrying..." warnings to see the retry mechanism in action.
 */
@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Main.class)
            .web(WebApplicationType.NONE)
            .run(args);
    }

    @Bean
    CommandLineRunner runner(WeatherAgent agent) {
        return args -> {
            System.out.println("=== Spring AI Tools Utils — Retry Example ===\n");

            System.out.println("=== Test 1: flaky tool succeeds after retries ===");
            System.out.println(agent.run("What is the weather in Amsterdam?"));

            System.out.println("\n=== Test 2: tool without retry annotation ===");
            System.out.println(agent.run("Give me the 5-day forecast for Amsterdam"));
        };
    }
}
