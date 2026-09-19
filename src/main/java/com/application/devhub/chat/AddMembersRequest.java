package com.application.devhub.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record AddMembersRequest(
        @Schema(description = "Developers to add to the group")
        @NotNull
        @Size(min = 1)
        List<UUID> userIds) {
}
