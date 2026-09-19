package com.application.devhub.session;

import java.util.UUID;

public record Rotation(UUID userId, IssuedRefreshToken refreshToken) {
}
