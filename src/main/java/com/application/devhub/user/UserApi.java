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
import static com.application.devhub.common.api.ErrorCode.USERNAME_CHANGE_TOO_SOON;
import static com.application.devhub.common.api.ErrorCode.USERNAME_TAKEN;

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

    @Operation(summary = "Change the signed-in user's username",
            description = "Allowed once per cooldown period (30 days by default). The old handle is held for the "
                    + "user for 14 days: nobody else can take it, its profile link keeps working, and the user can "
                    + "switch back to it without waiting for the cooldown. The old handle stops working for login. "
                    + "USERNAME_CHANGE_TOO_SOON says when the next change is allowed. Safe to repeat.")
    @ApiResponse(responseCode = "200", description = "The user with the new username", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, USERNAME_TAKEN, USERNAME_CHANGE_TOO_SOON})
    ApiEnvelope<CurrentUserResponse> changeUsername(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                                    ChangeUsernameRequest request);

    @Operation(summary = "Check whether a username is available",
            description = "For live feedback while typing a new username. The caller's own current or held "
                    + "username counts as available. INVALID when it breaks the username rules.")
    @ApiResponse(responseCode = "200", description = "Availability of the username", useReturnTypeSchema = true)
    ApiEnvelope<UsernameAvailabilityResponse> usernameAvailability(
            @Parameter(hidden = true) JwtAuthenticationToken authentication,
            @Parameter(description = "The username to check", example = "ada_builds") String username);
}
