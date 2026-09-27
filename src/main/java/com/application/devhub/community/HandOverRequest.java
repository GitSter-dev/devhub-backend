package com.application.devhub.community;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record HandOverRequest(
        @Schema(description = "The moderator who becomes the new owner")
        @NotNull
        UUID userId) {
}
