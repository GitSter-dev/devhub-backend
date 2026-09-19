package com.application.devhub.idempotency;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("devhub.idempotency")
public record IdempotencyProperties(Duration ttl) {
}
