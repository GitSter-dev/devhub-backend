package com.application.devhub.community;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.moderation.CaseStatus;
import com.application.devhub.moderation.ModerationAction;
import com.application.devhub.moderation.ModerationActionRepository;
import com.application.devhub.moderation.ModerationActionType;
import com.application.devhub.moderation.ModerationCase;
import com.application.devhub.moderation.ModerationCaseRepository;
import com.application.devhub.moderation.ReportRepository;
import com.application.devhub.moderation.ReportTarget;
import com.application.devhub.notification.NotificationType;
import com.application.devhub.notification.NotificationWriter;
import com.application.devhub.post.Post;
import com.application.devhub.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityModerationService {

    private static final int MAX_PINS = 3;

    private final CommunityAccess access;
    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;
    private final CommunityBanRepository banRepository;
    private final CommunityJoinRequestRepository requestRepository;
    private final PostRepository postRepository;
    private final ModerationCaseRepository caseRepository;
    private final ModerationActionRepository actionRepository;
    private final ReportRepository reportRepository;
    private final NotificationWriter notificationWriter;

    @Transactional
    public void removePost(UUID moderatorId, String slug, UUID postId, String note) {
        Community community = moderated(moderatorId, slug);
        Post post = postIn(community, postId);
        remove(community, post, moderatorId, note, null);
        openCaseFor(post.getId()).ifPresent(moderationCase -> resolve(moderationCase, moderatorId, CaseStatus.ACTIONED));
    }

    @Transactional
    public void restorePost(UUID moderatorId, String slug, UUID postId, String note) {
        Community community = moderated(moderatorId, slug);
        Post post = postIn(community, postId);
        if (!post.isRemoved()) {
            return;
        }
        if (!post.restoreToCommunity()) {
            throw ApiException.forbidden();
        }
        log(community, null, moderatorId, ModerationActionType.RESTORE, post.getAuthorId(), post.getId(), note, null);
    }

    @Transactional
    public void pin(UUID moderatorId, String slug, UUID postId) {
        Community community = moderated(moderatorId, slug);
        Post post = postIn(community, postId);
        if (!post.isLiveTopLevel()) {
            throw ApiException.badRequest();
        }
        if (post.getPinnedAt() == null && postRepository.countByCommunityIdAndPinnedAtIsNotNull(community.getId()) >= MAX_PINS) {
            throw ApiException.of(ErrorCode.PIN_LIMIT_REACHED);
        }
        post.pin();
    }

    @Transactional
    public void unpin(UUID moderatorId, String slug, UUID postId) {
        Community community = moderated(moderatorId, slug);
        postIn(community, postId).unpin();
    }

    @Transactional
    public void ban(UUID moderatorId, String slug, UUID userId, CommunityBanRequest request) {
        Community community = moderated(moderatorId, slug);
        banFrom(community, moderatorId, userId, request.reason(), request.days(), null);
    }

    @Transactional
    public void unban(UUID moderatorId, String slug, UUID userId, String note) {
        Community community = moderated(moderatorId, slug);
        CommunityMember.Key key = new CommunityMember.Key(community.getId(), userId);
        if (banRepository.existsById(key)) {
            banRepository.deleteById(key);
            log(community, null, moderatorId, ModerationActionType.COMMUNITY_UNBAN, userId, null, note, null);
        }
    }

    @Transactional
    public void act(UUID moderatorId, String slug, UUID caseId, CommunityCaseActionRequest request) {
        Community community = moderated(moderatorId, slug);
        ModerationCase moderationCase = caseRepository.findById(caseId)
                .filter(candidate -> community.getId().equals(candidate.getCommunityId()))
                .filter(candidate -> candidate.getTargetType() == ReportTarget.POST)
                .orElseThrow(ApiException::notFound);
        Post post = postIn(community, moderationCase.getTargetId());
        switch (request.action()) {
            case DISMISS -> {
                if (post.getHiddenAt() != null) {
                    post.restoreToCommunity();
                }
                moderationCase.clearAutoHide();
                moderationCase.resolve(moderatorId, CaseStatus.DISMISSED);
                actionRepository.save(ModerationAction.inCommunity(community.getId(), caseId, moderatorId,
                        ModerationActionType.DISMISS, post.getAuthorId(), post.getId(), request.note(), null));
            }
            case REMOVE_POST -> {
                remove(community, post, moderatorId, request.note(), caseId);
                resolve(moderationCase, moderatorId, CaseStatus.ACTIONED);
            }
            case BAN_AUTHOR -> {
                remove(community, post, moderatorId, request.note(), caseId);
                banFrom(community, moderatorId, post.getAuthorId(), request.note(), request.days(), caseId);
                resolve(moderationCase, moderatorId, CaseStatus.ACTIONED);
            }
        }
    }

    private void remove(Community community, Post post, UUID moderatorId, String note, UUID caseId) {
        if (post.isRemoved()) {
            return;
        }
        post.removeFromCommunity();
        log(community, caseId, moderatorId, ModerationActionType.REMOVE_CONTENT, post.getAuthorId(), post.getId(), note,
                null);
        notificationWriter.record(List.of(post.getAuthorId()), NotificationType.COMMUNITY_POST_REMOVED, post.getId(),
                moderatorId, post.getId());
    }

    private void banFrom(Community community, UUID moderatorId, UUID userId, String reason, Integer days, UUID caseId) {
        access.membership(community.getId(), userId).ifPresent(member -> {
            if (member.canModerate()) {
                throw ApiException.forbidden();
            }
            memberRepository.delete(member);
            communityRepository.adjustMemberCount(community.getId(), -1);
        });
        requestRepository.findByCommunityIdAndUserIdAndStatus(community.getId(), userId, JoinRequestStatus.PENDING)
                .ifPresent(pending -> pending.decline(moderatorId));
        Instant until = days == null ? null : Instant.now().plus(Duration.ofDays(days));
        banRepository.save(CommunityBan.of(community.getId(), userId, moderatorId, reason, until));
        log(community, caseId, moderatorId, ModerationActionType.COMMUNITY_BAN, userId, null, reason, until);
    }

    private void resolve(ModerationCase moderationCase, UUID moderatorId, CaseStatus outcome) {
        moderationCase.clearAutoHide();
        moderationCase.resolve(moderatorId, outcome);
        reportRepository.reporterIdsOf(moderationCase.getId()).forEach(reporter -> notificationWriter.record(
                List.of(reporter), NotificationType.REPORT_RESOLVED, moderationCase.getId(), reporter,
                moderationCase.getId()));
    }

    private Optional<ModerationCase> openCaseFor(UUID postId) {
        return caseRepository.findByTargetTypeAndTargetId(ReportTarget.POST, postId)
                .filter(moderationCase -> moderationCase.getStatus() == CaseStatus.OPEN);
    }

    private void log(Community community, UUID caseId, UUID moderatorId, ModerationActionType action, UUID targetUserId,
                     UUID targetPostId, String note, Instant until) {
        actionRepository.save(ModerationAction.inCommunity(community.getId(), caseId, moderatorId, action, targetUserId,
                targetPostId, note, until));
    }

    private Community moderated(UUID moderatorId, String slug) {
        Community community = access.live(slug);
        access.moderator(community.getId(), moderatorId);
        return community;
    }

    private Post postIn(Community community, UUID postId) {
        return postRepository.findById(postId)
                .filter(post -> community.getId().equals(post.getCommunityId()))
                .filter(post -> !post.isDeleted())
                .orElseThrow(ApiException::notFound);
    }
}
