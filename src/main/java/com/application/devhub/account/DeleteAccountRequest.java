package com.application.devhub.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @Schema(description = "Your current password, to prove it is you", example = "correct horse battery staple")
        @NotBlank
        String password) {
}
