package com.application.devhub.session;

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
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revocation_reason", length = 20)
    private RevocationReason revocationReason;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "issued_for_request", length = 64)
    private String issuedForRequest;

    public RefreshToken(UUID userId, UUID familyId, UUID parentId, String issuedForRequest, String tokenHash,
                        Instant expiresAt) {
        this.userId = userId;
        this.familyId = familyId;
        this.parentId = parentId;
        this.issuedForRequest = issuedForRequest;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean wasUsed() {
        return usedAt != null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired() {
        return !Instant.now().isBefore(expiresAt);
    }

    public boolean wasUsedAfter(Instant instant) {
        return usedAt != null && usedAt.isAfter(instant);
    }

    public boolean wasIssuedFor(String requestHash) {
        return issuedForRequest != null && issuedForRequest.equals(requestHash);
    }

    public void markUsed() {
        this.usedAt = Instant.now();
    }

    public void revoke(RevocationReason reason) {
        this.revokedAt = Instant.now();
        this.revocationReason = reason;
    }
}
