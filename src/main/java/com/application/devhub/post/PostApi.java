package com.application.devhub.post;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.openapi.ApiErrors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

import static com.application.devhub.common.api.ErrorCode.BAD_REQUEST;
import static com.application.devhub.common.api.ErrorCode.FORBIDDEN;
import static com.application.devhub.common.api.ErrorCode.NOT_FOUND;

@Tag(name = "Posts", description = "Posts with optional code blocks, threaded replies, likes and the home feed")
public interface PostApi {

    @Operation(summary = "Publish a post or a reply",
            description = "Text (up to 500 characters), a code block (up to 4000), or both. Set replyToId to reply; "
                    + "replies can themselves be replied to without limit. NOT_FOUND when replying to a missing or "
                    + "deleted post. Send an Idempotency-Key so a retried request never posts twice.")
    @ApiResponse(responseCode = "201", description = "The new post", useReturnTypeSchema = true)
    @ApiErrors(NOT_FOUND)
    ResponseEntity<ApiEnvelope<PostView>> create(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                                 CreatePostRequest request);

    @Operation(summary = "Delete your post",
            description = "Clears the post's content but keeps its place in the thread, so replies to it stay "
                    + "replies and show it as deleted. Deleting twice is harmless. FORBIDDEN for someone else's post.")
    @ApiResponse(responseCode = "200", description = "Deleted", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, FORBIDDEN})
    ApiEnvelope<Void> delete(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                             @Parameter(description = "The post to delete") UUID postId);

    @Operation(summary = "Like a post", description = "Liking a post you already like changes nothing. "
            + "NOT_FOUND for missing or deleted posts.")
    @ApiResponse(responseCode = "200", description = "Liked", useReturnTypeSchema = true)
    @ApiErrors(NOT_FOUND)
    ApiEnvelope<Void> like(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                           @Parameter(description = "The post to like") UUID postId);

    @Operation(summary = "Remove your like", description = "Unliking a post you don't like changes nothing.")
    @ApiResponse(responseCode = "200", description = "Like removed", useReturnTypeSchema = true)
    ApiEnvelope<Void> unlike(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                             @Parameter(description = "The post to unlike") UUID postId);

    @Operation(summary = "The home feed",
            description = "Original posts only, 20 per page. First everything from people the user follows (and "
                    + "their own posts), newest first; after that, posts from people who share at least one topic "
                    + "with them, newest first. Pass nextCursor to continue; it's null once both are exhausted.")
    @ApiResponse(responseCode = "200", description = "A page of the feed", useReturnTypeSchema = true)
    @ApiErrors(BAD_REQUEST)
    ApiEnvelope<PostPage> feed(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                               @Parameter(description = "nextCursor from the previous page") String cursor);

    @Operation(summary = "Open a post with its thread context",
            description = "The post plus every post above it in the thread, oldest first. Deleted posts stay in "
                    + "place with deleted set and no content.")
    @ApiResponse(responseCode = "200", description = "The post and its ancestors", useReturnTypeSchema = true)
    @ApiErrors(NOT_FOUND)
    ApiEnvelope<ThreadResponse> thread(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                       @Parameter(description = "The post to open") UUID postId);

    @Operation(summary = "List direct replies to a post",
            description = "Oldest first, 20 per page. A deleted reply is only listed while it still has replies "
                    + "of its own.")
    @ApiResponse(responseCode = "200", description = "A page of replies", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, BAD_REQUEST})
    ApiEnvelope<PostPage> replies(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                  @Parameter(description = "The post whose replies to list") UUID postId,
                                  @Parameter(description = "nextCursor from the previous page") String cursor);

    @Operation(summary = "List a developer's posts",
            description = "Their original posts, newest first, 20 per page. Replies and deleted posts are left out.")
    @ApiResponse(responseCode = "200", description = "A page of posts", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, BAD_REQUEST})
    ApiEnvelope<PostPage> userPosts(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                    @Parameter(description = "The developer's username") String username,
                                    @Parameter(description = "nextCursor from the previous page") String cursor);
}
