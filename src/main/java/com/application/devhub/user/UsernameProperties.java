package com.application.devhub.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("devhub.username")
public record UsernameProperties(Duration changeCooldown, Duration hold) {
}
