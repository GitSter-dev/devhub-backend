package com.application.devhub.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record MarkSeenRequest(
        @Schema(description = "updatedAt of the newest notification on screen; anything that changed later stays unseen",
                example = "2026-09-19T18:30:00Z")
        @NotNull
        Instant until) {
}
