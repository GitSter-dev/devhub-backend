package com.application.devhub.community;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.openapi.ApiErrors;
import com.application.devhub.community.CommunityViews.CommunitySummary;
import com.application.devhub.community.CommunityViews.CommunityView;
import com.application.devhub.post.PostPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static com.application.devhub.common.api.ErrorCode.ACCOUNT_TOO_NEW;
import static com.application.devhub.common.api.ErrorCode.BAD_REQUEST;
import static com.application.devhub.common.api.ErrorCode.COMMUNITY_LIMIT_REACHED;
import static com.application.devhub.common.api.ErrorCode.COMMUNITY_SLUG_TAKEN;
import static com.application.devhub.common.api.ErrorCode.NOT_COMMUNITY_MODERATOR;
import static com.application.devhub.common.api.ErrorCode.NOT_FOUND;

@Tag(name = "Communities", description = "Communities people join and post into, run by their own moderators")
public interface CommunityApi {

    @Operation(summary = "Found a community",
            description = "The founder becomes its owner and first member. The slug is its permanent address. "
                    + "ACCOUNT_TOO_NEW for accounts younger than a week; COMMUNITY_LIMIT_REACHED once someone owns "
                    + "three. Send an Idempotency-Key so a retry never founds two.")
    @ApiResponse(responseCode = "201", description = "The new community", useReturnTypeSchema = true)
    @ApiErrors({COMMUNITY_SLUG_TAKEN, ACCOUNT_TOO_NEW, COMMUNITY_LIMIT_REACHED})
    ResponseEntity<ApiEnvelope<CommunityView>> create(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                                      CreateCommunityRequest request);

    @Operation(summary = "Search or browse communities",
            description = "With q, communities whose slug or name match, best match first and typo-tolerant. "
                    + "Without q, the largest communities.")
    @ApiResponse(responseCode = "200", description = "Matching communities", useReturnTypeSchema = true)
    ApiEnvelope<List<CommunitySummary>> search(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                               @Parameter(description = "Search text", example = "rust") String q,
                                               @Parameter(description = "At most this many, 1-50") int limit);

    @Operation(summary = "Communities to join",
            description = "Communities the user hasn't joined that share their topics, closest match first, "
                    + "then the largest.")
    @ApiResponse(responseCode = "200", description = "Suggested communities", useReturnTypeSchema = true)
    ApiEnvelope<List<CommunitySummary>> suggestions(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                                    @Parameter(description = "At most this many, 1-50") int limit);

    @Operation(summary = "Your communities", description = "Communities the user belongs to, most recently joined first.")
    @ApiResponse(responseCode = "200", description = "The user's communities", useReturnTypeSchema = true)
    ApiEnvelope<List<CommunitySummary>> mine(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                             @Parameter(description = "At most this many, 1-100") int limit);

    @Operation(summary = "Open a community",
            description = "Its details plus the viewer's standing: their role when they're a member, and whether "
                    + "they have a pending request to join.")
    @ApiResponse(responseCode = "200", description = "The community", useReturnTypeSchema = true)
    @ApiErrors(NOT_FOUND)
    ApiEnvelope<CommunityView> get(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                   @Parameter(description = "The community's slug", example = "rust-lang") String slug);

    @Operation(summary = "Edit a community",
            description = "Moderators only. Omitted fields stay as they are; an empty description clears it. "
                    + "The slug never changes.")
    @ApiResponse(responseCode = "200", description = "The updated community", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_COMMUNITY_MODERATOR})
    ApiEnvelope<CommunityView> update(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                      @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                                      UpdateCommunityRequest request);

    @Operation(summary = "A community's posts",
            description = "Original posts in the community, newest first, 20 per page. Anyone can read them.")
    @ApiResponse(responseCode = "200", description = "A page of posts", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, BAD_REQUEST})
    ApiEnvelope<PostPage> posts(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                                @Parameter(description = "nextCursor from the previous page") String cursor);
}
