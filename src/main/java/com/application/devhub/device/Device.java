package com.application.devhub.device;

import com.application.devhub.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "devices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Device extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "installation_id", nullable = false, length = 64, updatable = false)
    private String installationId;

    @Column(name = "session_family_id")
    private UUID sessionFamilyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Platform platform;

    @Column(name = "push_token")
    private String pushToken;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "app_version", length = 30)
    private String appVersion;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revocation_reason", length = 20)
    private DeviceRevocationReason revocationReason;

    public Device(String installationId) {
        this.installationId = installationId;
    }

    public void register(DeviceRegistration registration) {
        this.userId = registration.userId();
        this.sessionFamilyId = registration.sessionId();
        this.platform = registration.platform();
        this.pushToken = registration.pushToken();
        this.deviceName = registration.deviceName();
        this.appVersion = registration.appVersion();
        this.lastSeenAt = Instant.now();
        this.revokedAt = null;
        this.revocationReason = null;
    }

    public void releasePushToken(DeviceRevocationReason reason) {
        this.pushToken = null;
        this.revokedAt = Instant.now();
        this.revocationReason = reason;
    }
}
