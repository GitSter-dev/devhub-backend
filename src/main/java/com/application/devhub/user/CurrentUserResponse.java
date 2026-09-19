package com.application.devhub.user;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String username,
        String displayName,
        String email,
        Role role,
        boolean emailVerified,
        boolean setupCompleted,
        Instant usernameChangeAvailableAt,
        Instant createdAt) {

    public static CurrentUserResponse from(User user, Duration usernameChangeCooldown) {
        Instant availableAt = user.usernameChangeAvailableAt(usernameChangeCooldown);
        return new CurrentUserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(),
                user.getRole(), user.isEmailVerified(), user.isSetupCompleted(),
                availableAt == null || availableAt.isBefore(Instant.now()) ? null : availableAt, user.getCreatedAt());
    }
}
