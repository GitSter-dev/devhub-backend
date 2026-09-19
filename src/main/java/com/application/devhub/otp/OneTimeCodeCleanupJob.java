package com.application.devhub.otp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class OneTimeCodeCleanupJob {

    private final OneTimeCodeRepository repository;

    @Transactional
    @Scheduled(cron = "${devhub.scheduling.cleanup-cron}")
    public int purge() {
        Instant now = Instant.now();
        int removed = repository.deleteExpiredOutsideIssueWindow(now, now.minus(OneTimeCode.ISSUE_WINDOW));
        log.info("One-time code cleanup removed {} codes", removed);
        return removed;
    }
}
