package com.application.devhub.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupJob {

    private static final Duration EMPTY_GRACE = Duration.ofDays(1);

    private final NotificationRepository repository;
    private final NotificationProperties properties;

    @Transactional
    @Scheduled(cron = "${devhub.scheduling.cleanup-cron}")
    public int purge() {
        Instant now = Instant.now();
        int seen = repository.deleteSeenBefore(now.minus(properties.retention()));
        int empty = repository.deleteEmptyBefore(now.minus(EMPTY_GRACE));
        log.info("Notification cleanup removed {} seen and {} empty notifications", seen, empty);
        return seen + empty;
    }
}
