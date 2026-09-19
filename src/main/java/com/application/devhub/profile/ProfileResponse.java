package com.application.devhub.profile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String username,
        String displayName,
        String bio,
        String githubUsername,
        String websiteUrl,
        List<String> topics,
        long followerCount,
        long followingCount,
        Instant joinedAt,
        boolean me,
        boolean following,
        boolean followsYou) {
}
