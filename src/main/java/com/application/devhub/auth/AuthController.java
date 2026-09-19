package com.application.devhub.auth;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.idempotency.Idempotent;
import com.application.devhub.passwordreset.PasswordResetService;
import com.application.devhub.ratelimit.RateLimitPolicy;
import com.application.devhub.ratelimit.RateLimited;
import com.application.devhub.session.SessionService;
import com.application.devhub.session.TokenPair;
import com.application.devhub.verification.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final SignupService signupService;
    private final EmailVerificationService emailVerificationService;
    private final SessionService sessionService;
    private final PasswordResetService passwordResetService;

    @Override
    @PostMapping("/signup")
    @Idempotent
    @RateLimited(RateLimitPolicy.SIGNUP)
    public ResponseEntity<ApiEnvelope<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.ok(signupService.signup(request)));
    }

    @Override
    @PostMapping("/verify-email")
    @Idempotent
    @RateLimited(RateLimitPolicy.VERIFY_EMAIL)
    public ApiEnvelope<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verify(request.email(), request.code());
        return ApiEnvelope.ok();
    }

    @Override
    @PostMapping("/login")
    @RateLimited(RateLimitPolicy.LOGIN)
    public ApiEnvelope<TokenPair> login(@Valid @RequestBody LoginRequest request) {
        return ApiEnvelope.ok(sessionService.login(request.identifier(), request.password()));
    }

    @Override
    @PostMapping("/refresh")
    @RateLimited(RateLimitPolicy.REFRESH)
    public ApiEnvelope<TokenPair> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                          @RequestHeader(value = Idempotent.HEADER, required = false)
                                          String idempotencyKey) {
        return ApiEnvelope.ok(sessionService.refresh(request.refreshToken(), idempotencyKey));
    }

    @Override
    @PostMapping("/logout")
    @Idempotent
    public ApiEnvelope<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        sessionService.logout(request.refreshToken());
        return ApiEnvelope.ok();
    }

    @Override
    @PostMapping("/forgot-password")
    @Idempotent
    @RateLimited(RateLimitPolicy.FORGOT_PASSWORD)
    public ResponseEntity<ApiEnvelope<Void>> forgotPassword(@Valid @RequestBody EmailRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiEnvelope.ok());
    }

    @Override
    @PostMapping("/reset-password")
    @Idempotent
    @RateLimited(RateLimitPolicy.RESET_PASSWORD)
    public ApiEnvelope<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request.email(), request.code(), request.newPassword());
        return ApiEnvelope.ok();
    }

    @Override
    @PostMapping("/resend-verification")
    @Idempotent
    @RateLimited(RateLimitPolicy.RESEND_VERIFICATION)
    public ResponseEntity<ApiEnvelope<Void>> resendVerification(@Valid @RequestBody EmailRequest request) {
        emailVerificationService.resend(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiEnvelope.ok());
    }
}
