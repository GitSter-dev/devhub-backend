package com.application.devhub.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OpenDirectRequest(
        @Schema(description = "The developer to message")
        @NotNull
        UUID userId) {
}
