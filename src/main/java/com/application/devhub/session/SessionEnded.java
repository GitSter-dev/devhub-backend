package com.application.devhub.session;

import java.util.UUID;

public record SessionEnded(UUID familyId, RevocationReason reason) {
}
