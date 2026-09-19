package com.application.devhub.user;

import com.application.devhub.common.validation.Username;
import io.swagger.v3.oas.annotations.media.Schema;

public record ChangeUsernameRequest(
        @Schema(description = "The new public handle: letters, digits and underscores", example = "ada_builds")
        @Username
        String username) {

    public ChangeUsernameRequest {
        username = username == null ? null : username.strip();
    }
}
