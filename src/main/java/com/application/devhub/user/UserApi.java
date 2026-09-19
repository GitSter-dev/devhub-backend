package com.application.devhub.user;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.openapi.ApiErrors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static com.application.devhub.common.api.ErrorCode.NOT_FOUND;
import static com.application.devhub.common.api.ErrorCode.TOPICS_REQUIRED;

@Tag(name = "Users", description = "The signed-in user and, later, other developers' profiles")
public interface UserApi {

    @Operation(summary = "Get the signed-in user",
            description = "Returns the account behind the bearer token. NOT_FOUND means the account no longer "
                    + "exists: the client should clear its session.")
    @ApiResponse(responseCode = "200", description = "The signed-in user", useReturnTypeSchema = true)
    @ApiErrors(NOT_FOUND)
    ApiEnvelope<CurrentUserResponse> me(@Parameter(hidden = true) JwtAuthenticationToken authentication);

    @Operation(summary = "Finish account setup",
            description = "Marks the post-signup setup as done so the app stops showing it on any device. "
                    + "TOPICS_REQUIRED until the user has picked at least one topic. Safe to repeat.")
    @ApiResponse(responseCode = "200", description = "The user, with setupCompleted true", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, TOPICS_REQUIRED})
    ApiEnvelope<CurrentUserResponse> completeSetup(@Parameter(hidden = true) JwtAuthenticationToken authentication);
}
