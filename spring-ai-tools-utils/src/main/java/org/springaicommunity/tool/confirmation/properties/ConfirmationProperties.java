package org.springaicommunity.tool.confirmation.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for the tool-confirmation subsystem, bound from the
 * {@code tools.confirmation} prefix.
 *
 * @param maxPending maximum number of in-flight confirmation requests held in memory
 * @param timeout    how long to wait for a confirmation response before auto-rejecting
 */
@ConfigurationProperties(prefix = "tools.confirmation")
public record ConfirmationProperties(
    int maxPending,
    Duration timeout
) {
    /**
     * Creates an instance with sensible defaults: 1 000 pending confirmations and a
     * 60-second timeout.
     */
    public ConfirmationProperties() {
        this(1_000, Duration.ofSeconds(60));
    }
}
