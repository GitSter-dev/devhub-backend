package com.application.devhub.block;

import java.util.UUID;

public record BlockChanged(UUID blockerId, UUID blockedId, boolean blocked) {
}
