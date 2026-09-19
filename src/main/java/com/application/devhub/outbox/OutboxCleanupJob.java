package com.application.devhub.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxCleanupJob {

    private final OutboxEventRepository repository;
    private final OutboxProperties properties;

    @Transactional
    @Scheduled(cron = "${devhub.scheduling.cleanup-cron}")
    public int purge() {
        Instant now = Instant.now();
        int sent = repository.deleteProcessedBefore(OutboxStatus.SENT, now.minus(properties.sentRetention()));
        int failed = repository.deleteProcessedBefore(OutboxStatus.FAILED, now.minus(properties.failedRetention()));
        log.info("Outbox cleanup removed {} sent and {} failed events", sent, failed);
        return sent + failed;
    }
}
