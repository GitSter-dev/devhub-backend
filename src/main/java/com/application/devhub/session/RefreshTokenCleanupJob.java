package com.application.devhub.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private final RefreshTokenRepository repository;

    @Transactional
    @Scheduled(cron = "${devhub.scheduling.cleanup-cron}")
    public int purge() {
        int removed = repository.deleteExpiredBefore(Instant.now());
        log.info("Refresh token cleanup removed {} expired tokens", removed);
        return removed;
    }
}
