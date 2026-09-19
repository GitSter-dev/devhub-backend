package com.application.devhub.session;

import java.util.UUID;

public record UserSessionsEnded(UUID userId, RevocationReason reason) {
}
