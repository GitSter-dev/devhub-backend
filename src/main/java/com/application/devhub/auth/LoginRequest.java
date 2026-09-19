package com.application.devhub.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "Email or username, case-insensitive", example = "ada_dev")
        @NotBlank
        String identifier,

        @Schema(example = "correct-horse-battery")
        @NotBlank
        String password) {

    public LoginRequest {
        identifier = identifier == null ? null : identifier.strip();
    }
}
