package com.application.devhub.auth;

import com.application.devhub.common.validation.DisplayName;
import com.application.devhub.common.validation.Password;
import com.application.devhub.common.validation.Username;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record SignupRequest(
        @Schema(description = "Public handle: letters, digits and underscores", example = "ada_dev")
        @Username
        String username,

        @Schema(example = "Ada Lovelace")
        @DisplayName
        String displayName,

        @Schema(example = "ada@devhub.dev")
        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @Schema(example = "correct-horse-battery")
        @Password
        String password) {

    public SignupRequest {
        username = username == null ? null : username.strip();
        displayName = displayName == null ? null : displayName.strip();
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
