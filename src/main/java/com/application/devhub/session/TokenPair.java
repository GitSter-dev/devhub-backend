package com.application.devhub.session;

import com.application.devhub.security.AccessToken;

import java.time.Instant;

public record TokenPair(
        String tokenType,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt) {

    private static final String BEARER = "Bearer";

    public static TokenPair of(AccessToken accessToken, IssuedRefreshToken refreshToken) {
        return new TokenPair(BEARER, accessToken.value(), accessToken.expiresAt(),
                refreshToken.value(), refreshToken.expiresAt());
    }
}
