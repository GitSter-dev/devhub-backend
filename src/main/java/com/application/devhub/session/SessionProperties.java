package com.application.devhub.session;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("devhub.session")
public record SessionProperties(Duration refreshTokenTtl, Duration refreshReplayWindow) {
}
