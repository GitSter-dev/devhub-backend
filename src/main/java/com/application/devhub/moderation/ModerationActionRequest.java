package com.application.devhub.moderation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ModerationActionRequest(
        @Schema(description = "What to do with the case", example = "REMOVE_CONTENT")
        @NotNull
        ModerationActionType action,

        @Schema(description = "Why, for the audit log", example = "Repeated harassment after a warning")
        @Size(max = 500)
        String note,

        @Schema(description = "How many days a SUSPEND lasts", example = "7")
        @Positive
        @Max(365)
        Integer days) {
}
