package com.application.devhub.account;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.openapi.ApiErrors;
import com.application.devhub.session.TokenPair;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static com.application.devhub.common.api.ErrorCode.INVALID_CREDENTIALS;
import static com.application.devhub.common.api.ErrorCode.NOT_FOUND;
import static com.application.devhub.common.api.ErrorCode.VALIDATION_FAILED;

@Tag(name = "Account", description = "Deleting and restoring your account")
public interface AccountApi {

    @Operation(summary = "Delete your account",
            description = "Deactivates the account straight away and signs you out everywhere. Signing in during the "
                    + "grace period through /auth/restore brings it back. After the grace period a nightly job "
                    + "anonymizes it for good: the profile becomes a deleted user, posts and messages lose their "
                    + "content, likes, follows, devices and notifications go, groups are left, and the old handle "
                    + "stays reserved so nobody can impersonate you.")
    @ApiResponse(responseCode = "200", description = "Deletion scheduled", useReturnTypeSchema = true)
    @ApiErrors({INVALID_CREDENTIALS, VALIDATION_FAILED})
    ApiEnvelope<Void> requestDeletion(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                      DeleteAccountRequest request);

    @Operation(summary = "Restore an account scheduled for deletion",
            description = "Cancels a pending deletion and signs you back in. Only works inside the grace period, "
                    + "before the account has been anonymized.")
    @ApiResponse(responseCode = "200", description = "Restored, with fresh tokens", useReturnTypeSchema = true)
    @ApiErrors({INVALID_CREDENTIALS, NOT_FOUND, VALIDATION_FAILED})
    ApiEnvelope<TokenPair> restore(RestoreAccountRequest request);
}
