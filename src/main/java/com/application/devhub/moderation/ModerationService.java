package com.application.devhub.moderation;

import com.application.devhub.chat.Message;
import com.application.devhub.chat.MessageRepository;
import com.application.devhub.common.api.ApiException;
import com.application.devhub.notification.NotificationType;
import com.application.devhub.notification.NotificationWriter;
import com.application.devhub.post.Post;
import com.application.devhub.post.PostRepository;
import com.application.devhub.session.RefreshTokenService;
import com.application.devhub.session.RevocationReason;
import com.application.devhub.user.User;
import com.application.devhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModerationService {

    private final ModerationCaseRepository caseRepository;
    private final ReportRepository reportRepository;
    private final ModerationActionRepository actionRepository;
    private final PostRepository postRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final NotificationWriter notificationWriter;

    @Transactional
    public void act(UUID moderatorId, UUID caseId, ModerationActionRequest request) {
        ModerationCase moderationCase = caseRepository.findById(caseId).orElseThrow(ApiException::notFound);
        Instant until = null;
        switch (request.action()) {
            case DISMISS -> {
                restoreContent(moderationCase);
                moderationCase.resolve(moderatorId, CaseStatus.DISMISSED);
            }
            case REMOVE_CONTENT -> {
                removeContent(moderationCase);
                moderationCase.resolve(moderatorId, CaseStatus.ACTIONED);
                notifyReporters(moderationCase);
            }
            case RESTORE -> {
                restoreContent(moderationCase);
                moderationCase.resolve(moderatorId, CaseStatus.DISMISSED);
            }
            case WARN -> moderationCase.resolve(moderatorId, CaseStatus.ACTIONED);
            case SUSPEND -> {
                if (request.days() == null) {
                    throw ApiException.badRequest();
                }
                until = Instant.now().plus(Duration.ofDays(request.days()));
                owner(moderationCase).suspendUntil(until);
                endSessions(moderationCase.getOwnerId());
                moderationCase.resolve(moderatorId, CaseStatus.ACTIONED);
                notifyReporters(moderationCase);
            }
            case BAN -> {
                owner(moderationCase).ban();
                endSessions(moderationCase.getOwnerId());
                moderationCase.resolve(moderatorId, CaseStatus.ACTIONED);
                notifyReporters(moderationCase);
            }
            case REINSTATE -> {
                owner(moderationCase).reinstate();
                moderationCase.resolve(moderatorId, CaseStatus.DISMISSED);
            }
        }
        actionRepository.save(ModerationAction.of(caseId, moderatorId, request.action(), moderationCase.getOwnerId(),
                request.note(), until));
    }

    private void removeContent(ModerationCase moderationCase) {
        switch (moderationCase.getTargetType()) {
            case POST -> post(moderationCase).remove();
            case MESSAGE -> message(moderationCase).remove();
            case USER -> throw ApiException.badRequest();
        }
        moderationCase.clearAutoHide();
        notificationWriter.withdrawSubject(moderationCase.getTargetId());
    }

    private void restoreContent(ModerationCase moderationCase) {
        switch (moderationCase.getTargetType()) {
            case POST -> post(moderationCase).restore();
            case MESSAGE -> message(moderationCase).restore();
            case USER -> {
            }
        }
        moderationCase.clearAutoHide();
    }

    private void notifyReporters(ModerationCase moderationCase) {
        List<UUID> reporters = reportRepository.reporterIdsOf(moderationCase.getId());
        reporters.forEach(reporter -> notificationWriter.record(List.of(reporter), NotificationType.REPORT_RESOLVED,
                moderationCase.getId(), reporter, moderationCase.getId()));
    }

    private void endSessions(UUID userId) {
        refreshTokenService.revokeAllFor(userId, RevocationReason.SUSPENDED);
    }

    private Post post(ModerationCase moderationCase) {
        return postRepository.findById(moderationCase.getTargetId()).orElseThrow(ApiException::notFound);
    }

    private Message message(ModerationCase moderationCase) {
        return messageRepository.findById(moderationCase.getTargetId()).orElseThrow(ApiException::notFound);
    }

    private User owner(ModerationCase moderationCase) {
        return userRepository.findById(moderationCase.getOwnerId()).orElseThrow(ApiException::notFound);
    }
}
