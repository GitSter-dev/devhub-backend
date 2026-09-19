package com.application.devhub.follow;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.ratelimit.RateLimitPolicy;
import com.application.devhub.ratelimit.RateLimitScope;
import com.application.devhub.ratelimit.RateLimited;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users/me/following")
@RequiredArgsConstructor
public class FollowController implements FollowApi {

    private final FollowService followService;

    @Override
    @PutMapping("/{userId}")
    @RateLimited(value = RateLimitPolicy.FOLLOW, scope = RateLimitScope.USER)
    public ApiEnvelope<Void> follow(JwtAuthenticationToken authentication, @PathVariable UUID userId) {
        followService.follow(userIdOf(authentication), userId);
        return ApiEnvelope.ok();
    }

    @Override
    @DeleteMapping("/{userId}")
    @RateLimited(value = RateLimitPolicy.FOLLOW, scope = RateLimitScope.USER)
    public ApiEnvelope<Void> unfollow(JwtAuthenticationToken authentication, @PathVariable UUID userId) {
        followService.unfollow(userIdOf(authentication), userId);
        return ApiEnvelope.ok();
    }

    private static UUID userIdOf(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getName());
    }
}
