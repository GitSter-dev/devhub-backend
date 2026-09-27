package com.application.devhub.community;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CommunityBanRequest(
        @Schema(description = "Why, shown in the public moderation log", example = "Repeated spam after a warning")
        @Size(max = 500)
        String reason,
        @Schema(description = "Ban length in days, 1-365; omit for a permanent ban", example = "30")
        @Min(1)
        @Max(365)
        Integer days) {

    public CommunityBanRequest {
        reason = reason == null || reason.isBlank() ? null : reason.strip();
    }
}
