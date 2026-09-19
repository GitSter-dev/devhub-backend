package com.application.devhub.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyRecordCleanupJob {

    private final IdempotencyRecordRepository repository;

    @Transactional
    @Scheduled(cron = "${devhub.scheduling.cleanup-cron}")
    public int purge() {
        int removed = repository.deleteExpiredBefore(Instant.now());
        log.info("Idempotency cleanup removed {} expired records", removed);
        return removed;
    }
}
