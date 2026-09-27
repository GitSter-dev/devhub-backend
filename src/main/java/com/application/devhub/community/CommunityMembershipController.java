package com.application.devhub.community;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.api.ApiException;
import com.application.devhub.community.CommunityViews.CommunityView;
import com.application.devhub.community.MemberQueries.JoinRequestPage;
import com.application.devhub.community.MemberQueries.MemberPage;
import com.application.devhub.ratelimit.RateLimitPolicy;
import com.application.devhub.ratelimit.RateLimitScope;
import com.application.devhub.ratelimit.RateLimited;
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

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CommunityMembershipController implements CommunityMembershipApi {

    private final MembershipService membershipService;
    private final CommunityViews communityViews;
    private final MemberQueries memberQueries;
    private final CommunityAccess access;

    @Override
    @PutMapping("/communities/{slug}/membership")
    @RateLimited(value = RateLimitPolicy.COMMUNITY_MEMBERSHIP, scope = RateLimitScope.USER)
    public ApiEnvelope<CommunityView> join(JwtAuthenticationToken authentication, @PathVariable String slug,
                                           @Valid @RequestBody(required = false) JoinCommunityRequest request) {
        UUID userId = userIdOf(authentication);
        membershipService.join(userId, slug, request == null ? new JoinCommunityRequest(null) : request);
        return ApiEnvelope.ok(view(userId, slug));
    }

    @Override
    @DeleteMapping("/communities/{slug}/membership")
    @RateLimited(value = RateLimitPolicy.COMMUNITY_MEMBERSHIP, scope = RateLimitScope.USER)
    public ApiEnvelope<CommunityView> leave(JwtAuthenticationToken authentication, @PathVariable String slug) {
        UUID userId = userIdOf(authentication);
        membershipService.leave(userId, slug);
        return ApiEnvelope.ok(view(userId, slug));
    }

    @Override
    @GetMapping("/communities/{slug}/members")
    public ApiEnvelope<MemberPage> members(JwtAuthenticationToken authentication, @PathVariable String slug,
                                           @RequestParam(required = false) CommunityRole role,
                                           @RequestParam(required = false) String cursor) {
        Community community = access.live(slug);
        return ApiEnvelope.ok(memberQueries.members(userIdOf(authentication), community.getId(), role, cursor));
    }

    @Override
    @GetMapping("/communities/{slug}/join-requests")
    public ApiEnvelope<JoinRequestPage> requests(JwtAuthenticationToken authentication, @PathVariable String slug,
                                                 @RequestParam(required = false) String cursor) {
        Community community = access.live(slug);
        access.moderator(community.getId(), userIdOf(authentication));
        return ApiEnvelope.ok(memberQueries.requests(community.getId(), cursor));
    }

    @Override
    @PostMapping("/communities/{slug}/join-requests/{requestId}/approve")
    public ApiEnvelope<Void> approve(JwtAuthenticationToken authentication, @PathVariable String slug,
                                     @PathVariable UUID requestId) {
        membershipService.approve(userIdOf(authentication), slug, requestId);
        return ApiEnvelope.ok();
    }

    @Override
    @PostMapping("/communities/{slug}/join-requests/{requestId}/decline")
    public ApiEnvelope<Void> decline(JwtAuthenticationToken authentication, @PathVariable String slug,
                                     @PathVariable UUID requestId) {
        membershipService.decline(userIdOf(authentication), slug, requestId);
        return ApiEnvelope.ok();
    }

    private CommunityView view(UUID viewerId, String slug) {
        return communityViews.bySlug(viewerId, slug).orElseThrow(ApiException::notFound);
    }

    private static UUID userIdOf(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getName());
    }
}
