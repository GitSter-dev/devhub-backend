package com.application.devhub.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("devhub.outbox")
public record OutboxProperties(
        Duration pollInterval,
        int batchSize,
        int maxAttempts,
        Duration retryBackoff,
        Duration sentRetention,
        Duration failedRetention) {
}
