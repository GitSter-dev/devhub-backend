package com.application.devhub.follow;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.openapi.ApiErrors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

import static com.application.devhub.common.api.ErrorCode.CANNOT_FOLLOW_SELF;
import static com.application.devhub.common.api.ErrorCode.NOT_FOUND;

@Tag(name = "Follows", description = "One-way follows between developers")
public interface FollowApi {

    @Operation(summary = "Follow a developer",
            description = "Starts following the user. Following someone you already follow succeeds and changes "
                    + "nothing. NOT_FOUND when the user doesn't exist or hasn't verified their email.")
    @ApiResponse(responseCode = "200", description = "Now following", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, CANNOT_FOLLOW_SELF})
    ApiEnvelope<Void> follow(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                             @Parameter(description = "The user to follow") UUID userId);

    @Operation(summary = "Unfollow a developer",
            description = "Stops following the user. Unfollowing someone you don't follow succeeds and changes nothing.")
    @ApiResponse(responseCode = "200", description = "No longer following", useReturnTypeSchema = true)
    ApiEnvelope<Void> unfollow(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                               @Parameter(description = "The user to unfollow") UUID userId);
}
