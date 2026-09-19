package com.application.devhub.auth;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.openapi.ApiErrors;
import com.application.devhub.session.TokenPair;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import static com.application.devhub.common.api.ErrorCode.CONFLICT;
import static com.application.devhub.common.api.ErrorCode.EMAIL_NOT_VERIFIED;
import static com.application.devhub.common.api.ErrorCode.EMAIL_TAKEN;
import static com.application.devhub.common.api.ErrorCode.INVALID_CREDENTIALS;
import static com.application.devhub.common.api.ErrorCode.INVALID_REFRESH_TOKEN;
import static com.application.devhub.common.api.ErrorCode.INVALID_RESET_CODE;
import static com.application.devhub.common.api.ErrorCode.INVALID_VERIFICATION_CODE;
import static com.application.devhub.common.api.ErrorCode.SESSION_REPLACED;
import static com.application.devhub.common.api.ErrorCode.USERNAME_TAKEN;

@Tag(name = "Auth", description = "Account creation, email verification, sessions and password recovery")
@SecurityRequirements
public interface AuthApi {

    @Operation(summary = "Create an account",
            description = "Creates an unverified account and queues a 6-digit verification code email. "
                    + "Never returns tokens: the user must verify their email, then log in.")
    @ApiResponse(responseCode = "201", description = "Account created", useReturnTypeSchema = true)
    @ApiErrors({EMAIL_TAKEN, USERNAME_TAKEN, CONFLICT})
    ResponseEntity<ApiEnvelope<SignupResponse>> signup(SignupRequest request);

    @Operation(summary = "Verify an email address",
            description = "Confirms the account with the emailed 6-digit code. After too many wrong attempts the "
                    + "code is invalidated and a new one must be requested. Unknown, already-verified and wrong-code "
                    + "cases return the identical error, so the endpoint reveals nothing about accounts.")
    @ApiResponse(responseCode = "200", description = "Email verified", useReturnTypeSchema = true)
    @ApiErrors(INVALID_VERIFICATION_CODE)
    ApiEnvelope<Void> verifyEmail(VerifyEmailRequest request);

    @Operation(summary = "Resend the verification code",
            description = "Always answers 202. A new code is only sent to an existing, unverified account that is "
                    + "outside its resend cooldown and under its daily limit.")
    @ApiResponse(responseCode = "202", description = "Request accepted", useReturnTypeSchema = true)
    ResponseEntity<ApiEnvelope<Void>> resendVerification(EmailRequest request);

    @Operation(summary = "Log in",
            description = "Authenticates with email or username (case-insensitive) and returns an access token and "
                    + "a refresh token. Starting a session signs out any other device (last login wins). "
                    + "Repeated failures lock the identifier temporarily (429).")
    @ApiResponse(responseCode = "200", description = "Logged in", useReturnTypeSchema = true)
    @ApiErrors({INVALID_CREDENTIALS, EMAIL_NOT_VERIFIED})
    ApiEnvelope<TokenPair> login(LoginRequest request);

    @Operation(summary = "Refresh the session",
            description = "Exchanges a refresh token for a new pair. Every refresh token works once. Reusing an old "
                    + "one ends the session. SESSION_REPLACED means another device logged in. "
                    + "Clients must send refresh requests one at a time. Send an Idempotency-Key: if the response is lost, "
                    + "retrying the same refresh with the same key within the replay window returns a fresh pair instead "
                    + "of being treated as token theft.")
    @ApiResponse(responseCode = "200", description = "New token pair", useReturnTypeSchema = true)
    @ApiErrors({INVALID_REFRESH_TOKEN, SESSION_REPLACED})
    ApiEnvelope<TokenPair> refresh(RefreshTokenRequest request,
                                   @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER,
                                           description = "One key per logical refresh, reused by its retries")
                                   String idempotencyKey);

    @Operation(summary = "Log out",
            description = "Ends the session that owns this refresh token. Always answers 200, even for unknown "
                    + "tokens. The access token stays valid until it expires.")
    @ApiResponse(responseCode = "200", description = "Logged out", useReturnTypeSchema = true)
    ApiEnvelope<Void> logout(RefreshTokenRequest request);

    @Operation(summary = "Request a password reset code",
            description = "Always answers 202. A code is only emailed to an existing, verified account that is "
                    + "outside its cooldown and under its daily limit.")
    @ApiResponse(responseCode = "202", description = "Request accepted", useReturnTypeSchema = true)
    ResponseEntity<ApiEnvelope<Void>> forgotPassword(EmailRequest request);

    @Operation(summary = "Reset the password",
            description = "Sets a new password using the emailed code, signs out every device and emails a "
                    + "'password changed' notice. Does not return tokens: log in with the new password.")
    @ApiResponse(responseCode = "200", description = "Password changed", useReturnTypeSchema = true)
    @ApiErrors(INVALID_RESET_CODE)
    ApiEnvelope<Void> resetPassword(ResetPasswordRequest request);
}
