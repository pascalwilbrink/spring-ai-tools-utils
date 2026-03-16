package org.springaicommunity;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

import java.nio.file.Files;
import java.nio.file.Path;

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
            System.out.println("=== Spring AI Tools Utils — Guardrails Example ===\n");

            // set up a test file with sensitive content
            Files.writeString(
                    Path.of("/tmp/config.txt"),
                    """
                    host=localhost
                    port=5432
                    password: s3cr3t123
                    """
            );

            System.out.println("=== Test 1: read file (output guardrail redacts secrets) ===");
            System.out.println(agent.run("Read the file at /tmp/config.txt"));

            System.out.println("\n=== Test 2: path traversal attempt (input guardrail blocks) ===");
            System.out.println(agent.run("Read the file at /tmp/../etc/passwd"));
        };
    }
}

