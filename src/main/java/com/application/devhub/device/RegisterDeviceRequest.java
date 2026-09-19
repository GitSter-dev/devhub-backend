package com.application.devhub.device;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterDeviceRequest(
        @Schema(description = "Random UUID the app generates once per install", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "may only contain letters, digits and dashes")
        String installationId,

        @Schema(example = "ANDROID")
        @NotNull
        Platform platform,

        @Schema(description = "FCM registration token", example = "fcm-registration-token")
        @NotBlank
        @Size(max = 4096)
        String pushToken,

        @Schema(example = "Pixel 8")
        @Size(max = 100)
        String deviceName,

        @Schema(example = "1.0.0")
        @Size(max = 30)
        String appVersion) {
}
