package com.application.devhub.profile;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.ratelimit.RateLimitPolicy;
import com.application.devhub.ratelimit.RateLimitScope;
import com.application.devhub.ratelimit.RateLimited;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class ProfileController implements ProfileApi {

    private static final int MAX_SEARCH_RESULTS = 50;

    private final ProfileService profileService;
    private final FollowListQuery followListQuery;
    private final PeopleSearch peopleSearch;

    @Override
    @GetMapping("/{username}/profile")
    public ApiEnvelope<ProfileResponse> profile(JwtAuthenticationToken authentication, @PathVariable String username) {
        return ApiEnvelope.ok(profileService.view(userIdOf(authentication), username));
    }

    @Override
    @PutMapping("/me/profile")
    public ApiEnvelope<ProfileResponse> updateProfile(JwtAuthenticationToken authentication,
                                                      @Valid @RequestBody UpdateProfileRequest request) {
        return ApiEnvelope.ok(profileService.update(userIdOf(authentication), request));
    }

    @Override
    @GetMapping("/{username}/followers")
    public ApiEnvelope<PersonPage> followers(JwtAuthenticationToken authentication, @PathVariable String username,
                                             @RequestParam(required = false) String cursor) {
        UUID targetId = profileService.visibleUser(userIdOf(authentication), username).getId();
        return ApiEnvelope.ok(followListQuery.followers(userIdOf(authentication), targetId, cursor));
    }

    @Override
    @GetMapping("/{username}/following")
    public ApiEnvelope<PersonPage> following(JwtAuthenticationToken authentication, @PathVariable String username,
                                             @RequestParam(required = false) String cursor) {
        UUID targetId = profileService.visibleUser(userIdOf(authentication), username).getId();
        return ApiEnvelope.ok(followListQuery.following(userIdOf(authentication), targetId, cursor));
    }

    @Override
    @GetMapping("/search")
    @RateLimited(value = RateLimitPolicy.SEARCH, scope = RateLimitScope.USER)
    public ApiEnvelope<List<PersonSummary>> search(JwtAuthenticationToken authentication,
                                                   @RequestParam(defaultValue = "") String q,
                                                   @RequestParam(defaultValue = "20") int limit) {
        return ApiEnvelope.ok(peopleSearch.search(userIdOf(authentication), q, Math.clamp(limit, 1, MAX_SEARCH_RESULTS)));
    }

    private static UUID userIdOf(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getName());
    }
}
