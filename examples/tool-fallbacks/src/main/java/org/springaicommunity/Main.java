package org.springaicommunity;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Main.class)
            .web(WebApplicationType.NONE)
            .run(args);
    }

    @Bean
    CommandLineRunner runner(DatabaseAgent agent) {
        return args -> {

            System.out.println("=== Test 1: fallback to method with cause ===");
            System.out.println(agent.run(
                    "Execute the query: SELECT * FROM users WHERE age > 18"
            ));

            System.out.println("\n=== Test 2: fallback to static message ===");
            System.out.println(agent.run(
                    "What is the current database status?"
            ));

            System.out.println("\n=== Test 3: fallback without cause ===");
            System.out.println(agent.run(
                    "List all available tables"
            ));

            System.out.println("\n=== Test 4: input guardrail blocks before fallback ===");
            System.out.println(agent.run(
                    "Execute the query: DROP TABLE users"
            ));
        };
    }
}

