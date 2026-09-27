package com.application.devhub.community;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record JoinCommunityRequest(
        @Schema(description = "A note for the moderators of a RESTRICTED community; ignored for OPEN ones",
                example = "I maintain a couple of embedded Rust crates.")
        @Size(max = 300)
        String message) {

    public JoinCommunityRequest {
        message = message == null || message.isBlank() ? null : message.strip();
    }
}
