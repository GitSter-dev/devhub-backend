package com.application.devhub.profile;

import com.application.devhub.common.validation.DisplayName;
import com.application.devhub.user.ProfileDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateProfileRequest(
        @Schema(example = "Ada Lovelace")
        @DisplayName
        String displayName,

        @Schema(description = "Up to 160 characters; blank clears it", example = "Rust and Go, mostly backends.")
        @Size(max = 160)
        String bio,

        @Schema(description = "GitHub username, not a URL; blank clears it", example = "octocat")
        @Size(max = 39)
        @Pattern(regexp = "^[A-Za-z0-9](?:[A-Za-z0-9]|-(?=[A-Za-z0-9]))*$", message = "is not a valid GitHub username")
        String githubUsername,

        @Schema(description = "An https:// link; blank clears it", example = "https://ada.dev")
        @Size(max = 200)
        @URL(protocol = "https", message = "must be an https:// link")
        String websiteUrl) {

    public UpdateProfileRequest {
        displayName = displayName == null ? null : displayName.strip();
        bio = blankToNull(bio);
        githubUsername = blankToNull(githubUsername);
        websiteUrl = blankToNull(websiteUrl);
    }

    ProfileDetails toDetails() {
        return new ProfileDetails(displayName, bio, githubUsername, websiteUrl);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
