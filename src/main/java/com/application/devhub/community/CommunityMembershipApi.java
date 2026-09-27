package com.application.devhub.community;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.openapi.ApiErrors;
import com.application.devhub.community.CommunityViews.CommunityView;
import com.application.devhub.community.MemberQueries.JoinRequestPage;
import com.application.devhub.community.MemberQueries.MemberPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

import static com.application.devhub.common.api.ErrorCode.BAD_REQUEST;
import static com.application.devhub.common.api.ErrorCode.NOT_COMMUNITY_MODERATOR;
import static com.application.devhub.common.api.ErrorCode.NOT_FOUND;
import static com.application.devhub.common.api.ErrorCode.OWNER_CANNOT_LEAVE;

@Tag(name = "Community membership", description = "Joining and leaving communities, members and join requests")
public interface CommunityMembershipApi {

    @Operation(summary = "Join a community",
            description = "OPEN communities admit the user at once. RESTRICTED ones record a request, with an "
                    + "optional message, for the moderators to decide. Joining again, or asking again while a "
                    + "request is pending, changes nothing.")
    @ApiResponse(responseCode = "200", description = "The community with the viewer's new standing",
            useReturnTypeSchema = true)
    @ApiErrors(NOT_FOUND)
    ApiEnvelope<CommunityView> join(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                    @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                                    JoinCommunityRequest request);

    @Operation(summary = "Leave a community or withdraw a request",
            description = "Leaves the community, or withdraws a pending request to join. Leaving when not a member "
                    + "changes nothing. OWNER_CANNOT_LEAVE until ownership is handed over.")
    @ApiResponse(responseCode = "200", description = "The community with the viewer's new standing",
            useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, OWNER_CANNOT_LEAVE})
    ApiEnvelope<CommunityView> leave(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                     @Parameter(description = "The community's slug", example = "rust-lang") String slug);

    @Operation(summary = "List a community's members",
            description = "Longest-standing first, 30 per page, optionally only one role. Blocked and deactivated "
                    + "accounts are left out.")
    @ApiResponse(responseCode = "200", description = "A page of members", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, BAD_REQUEST})
    ApiEnvelope<MemberPage> members(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                    @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                                    @Parameter(description = "Only members with this role") CommunityRole role,
                                    @Parameter(description = "nextCursor from the previous page") String cursor);

    @Operation(summary = "Pending requests to join",
            description = "Moderators only. Oldest first, 30 per page.")
    @ApiResponse(responseCode = "200", description = "A page of requests", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_COMMUNITY_MODERATOR, BAD_REQUEST})
    ApiEnvelope<JoinRequestPage> requests(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                          @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                                          @Parameter(description = "nextCursor from the previous page") String cursor);

    @Operation(summary = "Approve a request to join",
            description = "Moderators only. The requester becomes a member. NOT_FOUND once the request is decided "
                    + "or withdrawn.")
    @ApiResponse(responseCode = "200", description = "Approved", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_COMMUNITY_MODERATOR})
    ApiEnvelope<Void> approve(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                              @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                              @Parameter(description = "The request to approve") UUID requestId);

    @Operation(summary = "Decline a request to join",
            description = "Moderators only. The requester can ask again later. NOT_FOUND once the request is "
                    + "decided or withdrawn.")
    @ApiResponse(responseCode = "200", description = "Declined", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, NOT_COMMUNITY_MODERATOR})
    ApiEnvelope<Void> decline(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                              @Parameter(description = "The community's slug", example = "rust-lang") String slug,
                              @Parameter(description = "The request to decline") UUID requestId);
}
