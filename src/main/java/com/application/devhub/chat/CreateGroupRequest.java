package com.application.devhub.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateGroupRequest(
        @Schema(example = "Rust study group")
        @NotBlank
        @Size(max = 80)
        String title,

        @Schema(description = "Everyone to add besides yourself, 1 to 49 people")
        @NotNull
        @Size(min = 1)
        List<UUID> memberIds) {

    public CreateGroupRequest {
        title = title == null ? null : title.strip();
    }
}
