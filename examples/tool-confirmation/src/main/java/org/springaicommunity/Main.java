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
    CommandLineRunner runner(FileAgent agent) {
        return args -> {
            System.out.println("=== Spring AI Tools Utils — Confirmation Example ===\n");

            // This will trigger confirmation before writing
            String response = agent.run(
                    "Write 'Hello from Spring AI!' to /tmp/hello.txt, " +
                            "then read it back and tell me what it says"
            );

            System.out.println("\n=== Agent response ===");
            System.out.println(response);
        };
    }
}

