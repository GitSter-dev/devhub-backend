package com.application.devhub.community;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.openapi.ApiErrors;
import com.application.devhub.community.CommunityLogQuery.BanView;
import com.application.devhub.community.CommunityLogQuery.LogPage;
import com.application.devhub.moderation.CaseStatus;
import com.application.devhub.moderation.ModerationViews.CaseDetail;
import com.application.devhub.moderation.ModerationViews.CasePage;
import com.application.devhub.post.PostView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static com.application.devhub.common.api.ErrorCode.BAD_REQUEST;
import static com.application.devhub.common.api.ErrorCode.FORBIDDEN;
import static com.application.devhub.common.api.ErrorCode.NOT_COMMUNITY_MEMBER;
import static com.application.devhub.common.api.ErrorCode.NOT_COMMUNITY_MODERATOR;
import static com.application.devhub.common.api.ErrorCode.NOT_COMMUNITY_OWNER;
import static com.application.devhub.common.api.ErrorCode.NOT_FOUND;
import static com.application.devhub.common.api.ErrorCode.PIN_LIMIT_REACHED;

@Tag(name = "Community moderation", description = "Tools a community's owner and moderators use to run it")
public interface CommunityModerationApi {

    @Operation(summary = "Make a member a moderator", description = "Owner only. Promoting a moderator changes nothing.")
    @ApiResponse(responseCode = "200", description = "Appointed", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_COMMUNITY_OWNER, NOT_COMMUNITY_MEMBER})
    ApiEnvelope<Void> appoint(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                              @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                              @Parameter(description = "The member to promote") UUID userId);

    @Operation(summary = "Make a moderator a regular member again", description = "Owner only.")
    @ApiResponse(responseCode = "200", description = "Dismissed", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_COMMUNITY_OWNER, NOT_COMMUNITY_MEMBER})
    ApiEnvelope<Void> dismiss(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                              @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                              @Parameter(description = "The moderator to demote") UUID userId);

    @Operation(summary = "Hand the community over",
            description = "Owner only. The chosen moderator becomes the owner and the old owner stays on as a moderator.")
    @ApiResponse(responseCode = "200", description = "Handed over", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_COMMUNITY_OWNER, NOT_COMMUNITY_MODERATOR})
    ApiEnvelope<Void> handOver(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                               @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                               HandOverRequest request);

    @Operation(summary = "Replace the community's rules",
            description = "Moderators only. Sends the whole list in display order; reports can cite them.")
    @ApiResponse(responseCode = "200", description = "Rules saved", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_COMMUNITY_MODERATOR})
    ApiEnvelope<Void> rules(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                            @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                            CommunityRulesRequest request);

    @Operation(summary = "Pinned posts", description = "Up to three posts moderators keep at the top, newest first.")
    @ApiResponse(responseCode = "200", description = "The pinned posts", useReturnTypeSchema = true)
    @ApiErrors(NOT_FOUND)
    ApiEnvelope<List<PostView>> pins(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                     @Parameter(description = "The community's slug", example = "rust-lang") String slug);

    @Operation(summary = "Pin a post",
            description = "Moderators only, for original posts in this community. PIN_LIMIT_REACHED past three.")
    @ApiResponse(responseCode = "200", description = "Pinned", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_COMMUNITY_MODERATOR, PIN_LIMIT_REACHED, BAD_REQUEST})
    ApiEnvelope<Void> pin(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                          @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                          @Parameter(description = "The post to pin") UUID postId);

    @Operation(summary = "Unpin a post", description = "Moderators only.")
    @ApiResponse(responseCode = "200", description = "Unpinned", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_COMMUNITY_MODERATOR})
    ApiEnvelope<Void> unpin(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                            @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                            @Parameter(description = "The post to unpin") UUID postId);

    @Operation(summary = "Remove a post from the community",
            description = "Moderators only. The author is told and the removal appears in the public moderation "
                    + "log, without the post's text. Any open report about the post is resolved.")
    @ApiResponse(responseCode = "200", description = "Removed", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_COMMUNITY_MODERATOR})
    ApiEnvelope<Void> remove(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                             @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                             @Parameter(description = "The post to remove") UUID postId,
                             CommunityModerationNote request);

    @Operation(summary = "Restore a removed post",
            description = "Moderators only. FORBIDDEN when DevHub's own moderators removed it.")
    @ApiResponse(responseCode = "200", description = "Restored", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_COMMUNITY_MODERATOR, FORBIDDEN})
    ApiEnvelope<Void> restore(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                              @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                              @Parameter(description = "The post to restore") UUID postId);

    @Operation(summary = "Banned members", description = "Moderators only. Active bans, newest first.")
    @ApiResponse(responseCode = "200", description = "The active bans", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_COMMUNITY_MODERATOR})
    ApiEnvelope<List<BanView>> bans(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                    @Parameter(description = "The community's slug", example = "rust-lang") String slug);

    @Operation(summary = "Ban someone from the community",
            description = "Moderators only. Removes them from the community, declines any pending request, and "
                    + "stops them joining or posting until the ban ends. Moderators can't be banned.")
    @ApiResponse(responseCode = "200", description = "Banned", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_COMMUNITY_MODERATOR, FORBIDDEN})
    ApiEnvelope<Void> ban(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                          @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                          @Parameter(description = "Who to ban") UUID userId,
                          CommunityBanRequest request);

    @Operation(summary = "Lift a ban", description = "Moderators only. Lifting a ban that doesn't exist changes nothing.")
    @ApiResponse(responseCode = "200", description = "Unbanned", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_COMMUNITY_MODERATOR})
    ApiEnvelope<Void> unban(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                            @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                            @Parameter(description = "Who to unban") UUID userId);

    @Operation(summary = "Reported posts in the community",
            description = "Moderators only. Reports about this community's posts, most severe first, like DevHub's "
                    + "own queue. DevHub's moderators see them too.")
    @ApiResponse(responseCode = "200", description = "A page of cases", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_COMMUNITY_MODERATOR, BAD_REQUEST})
    ApiEnvelope<CasePage> cases(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                                @Parameter(description = "Which cases to list") CaseStatus status,
                                @Parameter(description = "nextCursor from the previous page") String cursor);

    @Operation(summary = "One reported post with its reports", description = "Moderators only.")
    @ApiResponse(responseCode = "200", description = "The case", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_COMMUNITY_MODERATOR})
    ApiEnvelope<CaseDetail> caseDetail(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                       @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                                       @Parameter(description = "The case to open") UUID caseId);

    @Operation(summary = "Act on a reported post",
            description = "Moderators only. DISMISS keeps the post, REMOVE_POST removes it, BAN_AUTHOR removes it "
                    + "and bans its author. Reporters are told once it's handled.")
    @ApiResponse(responseCode = "200", description = "The case after the action", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_COMMUNITY_MODERATOR, FORBIDDEN})
    ApiEnvelope<CaseDetail> act(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                                @Parameter(description = "The case to act on") UUID caseId,
                                CommunityCaseActionRequest request);

    @Operation(summary = "The public moderation log",
            description = "Anyone can read what moderators removed, restored, banned and unbanned, and why. "
                    + "Removed posts' text is never shown. Newest first, 30 per page.")
    @ApiResponse(responseCode = "200", description = "A page of log entries", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, BAD_REQUEST})
    ApiEnvelope<LogPage> log(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                             @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                             @Parameter(description = "nextCursor from the previous page") String cursor);
}
