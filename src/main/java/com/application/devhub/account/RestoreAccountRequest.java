package com.application.devhub.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RestoreAccountRequest(
        @Schema(description = "Username or email", example = "ada")
        @NotBlank
        String identifier,

        @Schema(description = "Your password", example = "correct horse battery staple")
        @NotBlank
        String password) {
}
