package com.application.devhub.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameGroupRequest(
        @Schema(example = "Rustaceans")
        @NotBlank
        @Size(max = 80)
        String title) {

    public RenameGroupRequest {
        title = title == null ? null : title.strip();
    }
}
