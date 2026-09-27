package com.application.devhub.community;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "community_bans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityBan {

    @EmbeddedId
    private CommunityMember.Key key;

    @Column(name = "banned_by", nullable = false)
    private UUID bannedBy;

    @Column(length = 500)
    private String reason;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    private CommunityBan(CommunityMember.Key key, UUID bannedBy, String reason, Instant expiresAt) {
        this.key = key;
        this.bannedBy = bannedBy;
        this.reason = reason;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public static CommunityBan of(UUID communityId, UUID userId, UUID bannedBy, String reason, Instant expiresAt) {
        return new CommunityBan(new CommunityMember.Key(communityId, userId), bannedBy, reason, expiresAt);
    }

    public boolean isActive() {
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }
}
