package com.application.devhub.moderation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("devhub.moderation")
public record ModerationProperties(
        int autoHideReporters,
        int reporterMinAgeDays,
        int reporterClearDays,
        int messageContextSize,
        int casePageSize,
        Duration reportRetention) {
}
