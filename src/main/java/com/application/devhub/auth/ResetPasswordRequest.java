package com.application.devhub.auth;

import com.application.devhub.common.validation.OneTimeCodeValue;
import com.application.devhub.common.validation.Password;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

public record ResetPasswordRequest(
        @Schema(example = "ada@devhub.dev")
        @NotBlank
        @Email
        String email,

        @Schema(description = "6-digit code from the email", example = "482913")
        @OneTimeCodeValue
        String code,

        @Schema(example = "a-brand-new-password")
        @Password
        String newPassword) {

    public ResetPasswordRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
        code = code == null ? null : code.strip();
    }
}
