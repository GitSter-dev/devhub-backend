package com.application.devhub.device;

import java.time.Instant;
import java.util.UUID;

public record DeviceResponse(
        UUID id,
        String installationId,
        Platform platform,
        String deviceName,
        String appVersion,
        Instant lastSeenAt) {

    public static DeviceResponse from(Device device) {
        return new DeviceResponse(device.getId(), device.getInstallationId(), device.getPlatform(),
                device.getDeviceName(), device.getAppVersion(), device.getLastSeenAt());
    }
}
