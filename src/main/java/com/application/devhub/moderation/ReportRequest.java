package com.application.devhub.moderation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ReportRequest(
        @Schema(description = "What is being reported", example = "POST")
        @NotNull
        ReportTarget targetType,

        @Schema(description = "The post, message or user being reported")
        @NotNull
        UUID targetId,

        @Schema(description = "Why it is being reported", example = "HARASSMENT")
        @NotNull
        ReportReason reason,

        @Schema(description = "Anything the moderators should know", example = "They keep targeting me in replies")
        @Size(max = 500)
        String note) {
}
