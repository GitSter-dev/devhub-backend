package com.application.devhub.community;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommunityCaseActionRequest(
        @Schema(description = "DISMISS keeps the post, REMOVE_POST takes it out of the community, BAN_AUTHOR also "
                + "bans its author from the community", example = "REMOVE_POST")
        @NotNull
        CommunityCaseAction action,
        @Schema(description = "Why, shown in the public moderation log", example = "Spam")
        @Size(max = 500)
        String note,
        @Schema(description = "For BAN_AUTHOR: ban length in days, 1-365; omit for permanent", example = "7")
        @Min(1)
        @Max(365)
        Integer days) {

    public CommunityCaseActionRequest {
        note = note == null || note.isBlank() ? null : note.strip();
    }
}
