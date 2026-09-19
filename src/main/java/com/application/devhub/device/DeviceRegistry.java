package com.application.devhub.device;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.session.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceRegistry {

    private final DeviceRepository repository;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public Device register(DeviceRegistration registration) {
        if (registration.sessionId() == null || !refreshTokenService.isSessionActive(registration.sessionId())) {
            throw ApiException.of(ErrorCode.SESSION_ENDED);
        }
        repository.findByPushToken(registration.pushToken())
                .filter(holder -> !holder.getInstallationId().equals(registration.installationId()))
                .ifPresent(holder -> {
                    holder.releasePushToken(DeviceRevocationReason.TOKEN_MOVED);
                    repository.saveAndFlush(holder);
                });
        Device device = repository.findByInstallationId(registration.installationId())
                .orElseGet(() -> new Device(registration.installationId()));
        device.register(registration);
        return repository.save(device);
    }

    @Transactional(readOnly = true)
    public List<Device> activeDevicesOf(UUID userId) {
        return repository.findActiveByUserId(userId);
    }

    @Transactional
    public void revokeInvalidToken(String pushToken) {
        repository.findByPushToken(pushToken)
                .ifPresent(device -> device.releasePushToken(DeviceRevocationReason.TOKEN_INVALID));
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        repository.revokeAllForUser(userId, Instant.now(), DeviceRevocationReason.SESSION_ENDED);
    }

    @Transactional
    public void revokeAllForSession(UUID familyId) {
        repository.revokeAllForSession(familyId, Instant.now(), DeviceRevocationReason.SESSION_ENDED);
    }
}
