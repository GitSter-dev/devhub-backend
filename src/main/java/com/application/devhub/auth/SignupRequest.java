package com.application.devhub.auth;

import com.application.devhub.common.validation.Password;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record SignupRequest(
        @Schema(description = "Public handle: letters, digits and underscores", example = "ada_dev")
        @NotBlank
        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "may only contain letters, digits and underscores")
        String username,

        @Schema(example = "Ada Lovelace")
        @NotBlank
        @Size(max = 50)
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
