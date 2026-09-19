package com.application.devhub.auth;

import com.application.devhub.user.Role;
import com.application.devhub.user.User;

import java.time.Instant;
import java.util.UUID;

public record SignupResponse(
        UUID id,
        String username,
        String displayName,
        String email,
        Role role,
        Instant createdAt) {

    public static SignupResponse from(User user) {
        return new SignupResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(),
                user.getRole(), user.getCreatedAt());
    }
}
