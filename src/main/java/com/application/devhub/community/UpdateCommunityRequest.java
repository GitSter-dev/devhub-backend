package com.application.devhub.community;

import com.application.devhub.topic.KnownTopics;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record UpdateCommunityRequest(
        @Schema(description = "New display name; omit to keep", example = "Rust")
        @Size(min = 3, max = 60)
        String name,
        @Schema(description = "New description; omit to keep, empty to clear")
        @Size(max = 500)
        String description,
        @Schema(description = "New join policy; omit to keep", example = "RESTRICTED")
        JoinPolicy joinPolicy,
        @Schema(description = "New topics, 1-3; omit to keep", example = "[\"rust\", \"webassembly\"]")
        @Size(min = 1, max = 3)
        @KnownTopics
        List<String> topics,
        @Schema(description = "Seconds members must wait between posts, 0 to turn slow mode off, at most 3600; "
                + "omit to keep", example = "60")
        @Min(0)
        @Max(3600)
        Integer slowModeSeconds) {

    public UpdateCommunityRequest {
        name = name == null ? null : name.strip();
        description = description == null ? null : description.strip();
        topics = topics == null ? null : topics.stream().filter(Objects::nonNull).map(String::strip).distinct().toList();
    }

    CommunityDetails appliedTo(Community community) {
        return new CommunityDetails(
                name != null ? name : community.getName(),
                description == null ? community.getDescription() : description.isEmpty() ? null : description,
                joinPolicy != null ? joinPolicy : community.getJoinPolicy(),
                topics != null ? new HashSet<>(topics) : community.getTopics(),
                slowModeSeconds != null ? slowModeSeconds : community.getSlowModeSeconds());
    }
}
