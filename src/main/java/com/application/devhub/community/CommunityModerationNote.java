package com.application.devhub.community;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record CommunityModerationNote(
        @Schema(description = "Why, shown in the public moderation log", example = "Off-topic: not about Rust")
        @Size(max = 500)
        String note) {

    public CommunityModerationNote {
        note = note == null || note.isBlank() ? null : note.strip();
    }
}
