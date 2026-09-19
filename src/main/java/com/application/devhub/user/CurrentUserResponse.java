package com.application.devhub.user;

import java.time.Instant;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String username,
        String displayName,
        String email,
        Role role,
        boolean emailVerified,
        Instant createdAt) {

    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(),
                user.getRole(), user.isEmailVerified(), user.getCreatedAt());
    }
}
