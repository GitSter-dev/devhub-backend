package com.application.devhub.moderation;

import com.application.devhub.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ModerationCaseRepository caseRepository;
    private final ReportSnapshots snapshots;
    private final ModerationProperties properties;
    private final PostRepository postRepository;

    @Transactional
    public void report(UUID reporterId, ReportRequest request) {
        ReportSnapshots.Captured captured = snapshots.capture(reporterId, request.targetType(), request.targetId());
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, request.targetType(),
                request.targetId())) {
            return;
        }
        ModerationCase moderationCase = caseRepository
                .findByTargetTypeAndTargetId(request.targetType(), request.targetId())
                .orElseGet(() -> caseRepository.saveAndFlush(
                        ModerationCase.open(request.targetType(), request.targetId(), captured.ownerId())));
        reportRepository.saveAndFlush(Report.of(moderationCase.getId(), reporterId, request.targetType(),
                request.targetId(), captured.ownerId(), request.reason(), request.note(), captured.json()));
        moderationCase.recordReport(request.reason().weight());
        autoHideIfBrigaded(moderationCase);
    }

    private void autoHideIfBrigaded(ModerationCase moderationCase) {
        if (moderationCase.getTargetType() != ReportTarget.POST || moderationCase.isAutoHidden()) {
            return;
        }
        long established = reportRepository.countEstablishedReporters(moderationCase.getId(),
                properties.reporterMinAgeDays(), properties.reporterClearDays());
        if (established < properties.autoHideReporters()) {
            return;
        }
        postRepository.findById(moderationCase.getTargetId()).ifPresent(post -> {
            post.hide();
            moderationCase.autoHide();
        });
    }
}
