package com.application.devhub.community;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.api.ApiException;
import com.application.devhub.community.CommunityLogQuery.BanView;
import com.application.devhub.community.CommunityLogQuery.LogPage;
import com.application.devhub.moderation.CaseStatus;
import com.application.devhub.moderation.ModerationQueries;
import com.application.devhub.moderation.ModerationViews.CaseDetail;
import com.application.devhub.moderation.ModerationViews.CasePage;
import com.application.devhub.post.PostView;
import com.application.devhub.post.PostViews;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CommunityModerationController implements CommunityModerationApi {

    private static final String PINNED = "p.community_id = :communityId AND p.pinned_at IS NOT NULL";

    private final CommunityAccess access;
    private final CommunityStaffService staffService;
    private final CommunityRulesService rulesService;
    private final CommunityModerationService moderationService;
    private final CommunityLogQuery logQuery;
    private final ModerationQueries moderationQueries;
    private final PostViews postViews;

    @Override
    @PutMapping("/communities/{slug}/moderators/{userId}")
    public ApiEnvelope<Void> appoint(JwtAuthenticationToken authentication, @PathVariable String slug,
                                     @PathVariable UUID userId) {
        staffService.appoint(userIdOf(authentication), slug, userId);
        return ApiEnvelope.ok();
    }

    @Override
    @DeleteMapping("/communities/{slug}/moderators/{userId}")
    public ApiEnvelope<Void> dismiss(JwtAuthenticationToken authentication, @PathVariable String slug,
                                     @PathVariable UUID userId) {
        staffService.dismiss(userIdOf(authentication), slug, userId);
        return ApiEnvelope.ok();
    }

    @Override
    @PostMapping("/communities/{slug}/ownership")
    public ApiEnvelope<Void> handOver(JwtAuthenticationToken authentication, @PathVariable String slug,
                                      @Valid @RequestBody HandOverRequest request) {
        staffService.handOver(userIdOf(authentication), slug, request.userId());
        return ApiEnvelope.ok();
    }

    @Override
    @PutMapping("/communities/{slug}/rules")
    public ApiEnvelope<Void> rules(JwtAuthenticationToken authentication, @PathVariable String slug,
                                   @Valid @RequestBody CommunityRulesRequest request) {
        rulesService.replace(userIdOf(authentication), slug, request);
        return ApiEnvelope.ok();
    }

    @Override
    @GetMapping("/communities/{slug}/pins")
    public ApiEnvelope<List<PostView>> pins(JwtAuthenticationToken authentication, @PathVariable String slug) {
        Community community = access.live(slug);
        return ApiEnvelope.ok(postViews.slice(userIdOf(authentication), PINNED, Map.of("communityId", community.getId()),
                null, false, 3));
    }

    @Override
    @PutMapping("/communities/{slug}/pins/{postId}")
    public ApiEnvelope<Void> pin(JwtAuthenticationToken authentication, @PathVariable String slug,
                                 @PathVariable UUID postId) {
        moderationService.pin(userIdOf(authentication), slug, postId);
        return ApiEnvelope.ok();
    }

    @Override
    @DeleteMapping("/communities/{slug}/pins/{postId}")
    public ApiEnvelope<Void> unpin(JwtAuthenticationToken authentication, @PathVariable String slug,
                                   @PathVariable UUID postId) {
        moderationService.unpin(userIdOf(authentication), slug, postId);
        return ApiEnvelope.ok();
    }

    @Override
    @PutMapping("/communities/{slug}/posts/{postId}/removal")
    public ApiEnvelope<Void> remove(JwtAuthenticationToken authentication, @PathVariable String slug,
                                    @PathVariable UUID postId,
                                    @Valid @RequestBody(required = false) CommunityModerationNote request) {
        moderationService.removePost(userIdOf(authentication), slug, postId, request == null ? null : request.note());
        return ApiEnvelope.ok();
    }

    @Override
    @DeleteMapping("/communities/{slug}/posts/{postId}/removal")
    public ApiEnvelope<Void> restore(JwtAuthenticationToken authentication, @PathVariable String slug,
                                     @PathVariable UUID postId) {
        moderationService.restorePost(userIdOf(authentication), slug, postId, null);
        return ApiEnvelope.ok();
    }

    @Override
    @GetMapping("/communities/{slug}/bans")
    public ApiEnvelope<List<BanView>> bans(JwtAuthenticationToken authentication, @PathVariable String slug) {
        return ApiEnvelope.ok(logQuery.bans(moderated(authentication, slug).getId()));
    }

    @Override
    @PutMapping("/communities/{slug}/bans/{userId}")
    public ApiEnvelope<Void> ban(JwtAuthenticationToken authentication, @PathVariable String slug,
                                 @PathVariable UUID userId,
                                 @Valid @RequestBody(required = false) CommunityBanRequest request) {
        moderationService.ban(userIdOf(authentication), slug, userId,
                request == null ? new CommunityBanRequest(null, null) : request);
        return ApiEnvelope.ok();
    }

    @Override
    @DeleteMapping("/communities/{slug}/bans/{userId}")
    public ApiEnvelope<Void> unban(JwtAuthenticationToken authentication, @PathVariable String slug,
                                   @PathVariable UUID userId) {
        moderationService.unban(userIdOf(authentication), slug, userId, null);
        return ApiEnvelope.ok();
    }

    @Override
    @GetMapping("/communities/{slug}/moderation/cases")
    public ApiEnvelope<CasePage> cases(JwtAuthenticationToken authentication, @PathVariable String slug,
                                       @RequestParam(defaultValue = "OPEN") CaseStatus status,
                                       @RequestParam(required = false) String cursor) {
        return ApiEnvelope.ok(moderationQueries.casesIn(status, moderated(authentication, slug).getId(), cursor));
    }

    @Override
    @GetMapping("/communities/{slug}/moderation/cases/{caseId}")
    public ApiEnvelope<CaseDetail> caseDetail(JwtAuthenticationToken authentication, @PathVariable String slug,
                                              @PathVariable UUID caseId) {
        return ApiEnvelope.ok(caseIn(moderated(authentication, slug), caseId));
    }

    @Override
    @PostMapping("/communities/{slug}/moderation/cases/{caseId}/actions")
    public ApiEnvelope<CaseDetail> act(JwtAuthenticationToken authentication, @PathVariable String slug,
                                       @PathVariable UUID caseId,
                                       @Valid @RequestBody CommunityCaseActionRequest request) {
        moderationService.act(userIdOf(authentication), slug, caseId, request);
        return ApiEnvelope.ok(caseIn(access.live(slug), caseId));
    }

    @Override
    @GetMapping("/communities/{slug}/moderation-log")
    public ApiEnvelope<LogPage> log(JwtAuthenticationToken authentication, @PathVariable String slug,
                                    @RequestParam(required = false) String cursor) {
        return ApiEnvelope.ok(logQuery.entries(access.live(slug).getId(), cursor));
    }

    private CaseDetail caseIn(Community community, UUID caseId) {
        CaseDetail detail = moderationQueries.caseDetail(caseId);
        if (detail.moderationCase().community() == null
                || !community.getId().equals(detail.moderationCase().community().id())) {
            throw ApiException.notFound();
        }
        return detail;
    }

    private Community moderated(JwtAuthenticationToken authentication, String slug) {
        Community community = access.live(slug);
        access.moderator(community.getId(), userIdOf(authentication));
        return community;
    }

    private static UUID userIdOf(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getName());
    }
}
