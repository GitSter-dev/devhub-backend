package com.application.devhub.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

public record EmailRequest(
        @Schema(example = "ada@devhub.dev")
        @NotBlank
        @Email
        String email) {

    public EmailRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
