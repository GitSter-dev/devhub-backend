package com.application.devhub.device;

import java.util.UUID;

public record DeviceRegistration(
        UUID userId,
        UUID sessionId,
        String installationId,
        Platform platform,
        String pushToken,
        String deviceName,
        String appVersion) {
}
