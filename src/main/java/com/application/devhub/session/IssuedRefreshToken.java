package com.application.devhub.session;

import java.time.Instant;
import java.util.UUID;

public record IssuedRefreshToken(String value, Instant expiresAt, UUID familyId) {
}
