package com.application.devhub.profile;

import java.util.UUID;

public record PersonSummary(UUID id, String username, String displayName, boolean following) {
}
