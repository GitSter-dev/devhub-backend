package com.application.devhub.moderation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModerationCleanupJob {

    private final ReportRepository reportRepository;
    private final ModerationCaseRepository caseRepository;
    private final ModerationProperties properties;

    @Transactional
    @Scheduled(cron = "${devhub.scheduling.cleanup-cron}")
    public int purge() {
        int reports = reportRepository.deleteReportsBefore(Instant.now().minus(properties.reportRetention()));
        int cases = caseRepository.deleteEmptyResolvedCases();
        log.info("Moderation cleanup removed {} reports and {} closed cases", reports, cases);
        return reports + cases;
    }
}
