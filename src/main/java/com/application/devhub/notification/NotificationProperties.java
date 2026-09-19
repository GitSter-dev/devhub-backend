package com.application.devhub.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("devhub.notifications")
public record NotificationProperties(
        Duration pushPollInterval,
        int pushBatchSize,
        Duration burstWindow,
        int individualPushes,
        Duration digestInterval,
        Duration postsDigestInterval,
        int fanOutChunk,
        int pageSize,
        Duration retention) {

    public Duration digestFor(NotificationType type) {
        return type == NotificationType.FOLLOWED_POSTED ? postsDigestInterval : digestInterval;
    }
}
