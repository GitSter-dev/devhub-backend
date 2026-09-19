package com.application.devhub.security;

import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {
}
