package com.application.devhub.community;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.api.ApiException;
import com.application.devhub.community.CommunityViews.CommunitySummary;
import com.application.devhub.community.CommunityViews.CommunityView;
import com.application.devhub.idempotency.Idempotent;
import com.application.devhub.post.PostPage;
import com.application.devhub.post.PostViews;
import com.application.devhub.ratelimit.RateLimitPolicy;
import com.application.devhub.ratelimit.RateLimitScope;
import com.application.devhub.ratelimit.RateLimited;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CommunityController implements CommunityApi {

    private static final int MAX_LIMIT = 50;
    private static final int MAX_JOINED = 100;
    private static final String COMMUNITY_POSTS = "p.community_id = :communityId AND p.parent_id IS NULL AND p.deleted_at IS NULL";

    private final CommunityService communityService;
    private final CommunityViews communityViews;
    private final CommunityAccess access;
    private final PostViews postViews;

    @Override
    @PostMapping("/communities")
    @Idempotent
    @RateLimited(value = RateLimitPolicy.COMMUNITY_CREATE, scope = RateLimitScope.USER)
    public ResponseEntity<ApiEnvelope<CommunityView>> create(JwtAuthenticationToken authentication,
                                                             @Valid @RequestBody CreateCommunityRequest request) {
        UUID userId = userIdOf(authentication);
        Community community = communityService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.ok(view(userId, community.getSlug())));
    }

    @Override
    @GetMapping("/communities")
    @RateLimited(value = RateLimitPolicy.SEARCH, scope = RateLimitScope.USER)
    public ApiEnvelope<List<CommunitySummary>> search(JwtAuthenticationToken authentication,
                                                      @RequestParam(required = false) String q,
                                                      @RequestParam(defaultValue = "20") int limit) {
        return ApiEnvelope.ok(communityViews.search(userIdOf(authentication), q, Math.clamp(limit, 1, MAX_LIMIT)));
    }

    @Override
    @GetMapping("/communities/suggestions")
    public ApiEnvelope<List<CommunitySummary>> suggestions(JwtAuthenticationToken authentication,
                                                           @RequestParam(defaultValue = "20") int limit) {
        return ApiEnvelope.ok(communityViews.suggested(userIdOf(authentication), Math.clamp(limit, 1, MAX_LIMIT)));
    }

    @Override
    @GetMapping("/users/me/communities")
    public ApiEnvelope<List<CommunitySummary>> mine(JwtAuthenticationToken authentication,
                                                    @RequestParam(defaultValue = "50") int limit) {
        return ApiEnvelope.ok(communityViews.joined(userIdOf(authentication), Math.clamp(limit, 1, MAX_JOINED)));
    }

    @Override
    @GetMapping("/communities/{slug}")
    public ApiEnvelope<CommunityView> get(JwtAuthenticationToken authentication, @PathVariable String slug) {
        return ApiEnvelope.ok(view(userIdOf(authentication), slug));
    }

    @Override
    @PatchMapping("/communities/{slug}")
    public ApiEnvelope<CommunityView> update(JwtAuthenticationToken authentication, @PathVariable String slug,
                                             @Valid @RequestBody UpdateCommunityRequest request) {
        UUID userId = userIdOf(authentication);
        communityService.update(userId, slug, request);
        return ApiEnvelope.ok(view(userId, slug));
    }

    @Override
    @GetMapping("/communities/{slug}/posts")
    public ApiEnvelope<PostPage> posts(JwtAuthenticationToken authentication, @PathVariable String slug,
                                       @RequestParam(required = false) String cursor) {
        Community community = access.live(slug);
        return ApiEnvelope.ok(postViews.page(userIdOf(authentication), COMMUNITY_POSTS,
                Map.of("communityId", community.getId()), cursor, false));
    }

    private CommunityView view(UUID viewerId, String slug) {
        return communityViews.bySlug(viewerId, slug).orElseThrow(ApiException::notFound);
    }

    private static UUID userIdOf(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getName());
    }
}
