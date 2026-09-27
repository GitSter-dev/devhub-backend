package com.application.devhub.community;

import com.application.devhub.topic.KnownTopics;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record CreateCommunityRequest(
        @Schema(description = "Permanent address, 3-30 lowercase letters, digits and inner dashes", example = "rust-lang")
        @NotBlank
        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[a-z0-9]([a-z0-9-]*[a-z0-9])?$",
                message = "may only contain lowercase letters, digits and dashes, and can't start or end with a dash")
        String slug,
        @Schema(description = "Display name, 3-60 characters", example = "Rust")
        @NotBlank
        @Size(min = 3, max = 60)
        String name,
        @Schema(description = "What the community is about, up to 500 characters",
                example = "Systems programming without fear. Questions, crates and war stories.")
        @Size(max = 500)
        String description,
        @Schema(description = "OPEN lets anyone join; RESTRICTED makes people ask", example = "OPEN")
        @NotNull
        JoinPolicy joinPolicy,
        @Schema(description = "1-3 topic slugs from GET /topics", example = "[\"rust\"]")
        @NotNull
        @Size(min = 1, max = 3)
        @KnownTopics
        List<String> topics) {

    public CreateCommunityRequest {
        slug = slug == null ? null : slug.strip().toLowerCase(Locale.ROOT);
        name = name == null ? null : name.strip();
        description = description == null || description.isBlank() ? null : description.strip();
        topics = topics == null ? null : topics.stream().filter(Objects::nonNull).map(String::strip).distinct().toList();
    }

    CommunityDetails details() {
        return new CommunityDetails(name, description, joinPolicy, Set.copyOf(topics), 0);
    }
}
