package com.application.devhub.post;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.api.ApiException;
import com.application.devhub.idempotency.Idempotent;
import com.application.devhub.profile.ProfileService;
import com.application.devhub.ratelimit.RateLimitPolicy;
import com.application.devhub.ratelimit.RateLimitScope;
import com.application.devhub.ratelimit.RateLimited;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class PostController implements PostApi {

    private final PostService postService;
    private final PostViews postViews;
    private final FeedQuery feedQuery;
    private final ThreadQuery threadQuery;
    private final ProfileService profileService;

    @Override
    @PostMapping("/posts")
    @Idempotent
    @RateLimited(value = RateLimitPolicy.POSTING, scope = RateLimitScope.USER)
    public ResponseEntity<ApiEnvelope<PostView>> create(JwtAuthenticationToken authentication,
                                                        @Valid @RequestBody CreatePostRequest request) {
        UUID userId = userIdOf(authentication);
        UUID postId = postService.create(userId, request);
        PostView post = postViews.byId(userId, postId).orElseThrow(ApiException::notFound);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.ok(post));
    }

    @Override
    @DeleteMapping("/posts/{postId}")
    public ApiEnvelope<Void> delete(JwtAuthenticationToken authentication, @PathVariable UUID postId) {
        postService.delete(userIdOf(authentication), postId);
        return ApiEnvelope.ok();
    }

    @Override
    @PutMapping("/posts/{postId}/like")
    @RateLimited(value = RateLimitPolicy.LIKE, scope = RateLimitScope.USER)
    public ApiEnvelope<Void> like(JwtAuthenticationToken authentication, @PathVariable UUID postId) {
        postService.like(userIdOf(authentication), postId);
        return ApiEnvelope.ok();
    }

    @Override
    @DeleteMapping("/posts/{postId}/like")
    @RateLimited(value = RateLimitPolicy.LIKE, scope = RateLimitScope.USER)
    public ApiEnvelope<Void> unlike(JwtAuthenticationToken authentication, @PathVariable UUID postId) {
        postService.unlike(userIdOf(authentication), postId);
        return ApiEnvelope.ok();
    }

    @Override
    @GetMapping("/feed")
    public ApiEnvelope<PostPage> feed(JwtAuthenticationToken authentication,
                                      @RequestParam(required = false) String cursor) {
        return ApiEnvelope.ok(feedQuery.feed(userIdOf(authentication), cursor));
    }

    @Override
    @GetMapping("/posts/{postId}")
    public ApiEnvelope<ThreadResponse> thread(JwtAuthenticationToken authentication, @PathVariable UUID postId) {
        return ApiEnvelope.ok(threadQuery.thread(userIdOf(authentication), postId));
    }

    @Override
    @GetMapping("/posts/{postId}/replies")
    public ApiEnvelope<PostPage> replies(JwtAuthenticationToken authentication, @PathVariable UUID postId,
                                         @RequestParam(required = false) String cursor) {
        return ApiEnvelope.ok(threadQuery.replies(userIdOf(authentication), postId, cursor));
    }

    @Override
    @GetMapping("/users/{username}/posts")
    public ApiEnvelope<PostPage> userPosts(JwtAuthenticationToken authentication, @PathVariable String username,
                                           @RequestParam(required = false) String cursor) {
        UUID authorId = profileService.visibleUser(username).getId();
        return ApiEnvelope.ok(threadQuery.byAuthor(userIdOf(authentication), authorId, cursor));
    }

    private static UUID userIdOf(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getName());
    }
}
