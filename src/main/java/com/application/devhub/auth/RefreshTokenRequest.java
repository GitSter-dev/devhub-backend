package com.application.devhub.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @Schema(example = "Qm9vdHN0cmFwUmVmcmVzaFRva2VuRXhhbXBsZTEyMzQ")
        @NotBlank
        String refreshToken) {

    public RefreshTokenRequest {
        refreshToken = refreshToken == null ? null : refreshToken.strip();
    }
}
