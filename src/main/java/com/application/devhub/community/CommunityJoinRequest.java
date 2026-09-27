package com.application.devhub.community;

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
@Table(name = "community_join_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityJoinRequest extends BaseEntity {

    @Column(name = "community_id", nullable = false, updatable = false)
    private UUID communityId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JoinRequestStatus status;

    @Column(length = 300, updatable = false)
    private String message;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    private CommunityJoinRequest(UUID communityId, UUID userId, String message) {
        this.communityId = communityId;
        this.userId = userId;
        this.message = message;
        this.status = JoinRequestStatus.PENDING;
    }

    public static CommunityJoinRequest ask(UUID communityId, UUID userId, String message) {
        return new CommunityJoinRequest(communityId, userId, message);
    }

    public boolean isPending() {
        return status == JoinRequestStatus.PENDING;
    }

    public void approve(UUID moderatorId) {
        decide(JoinRequestStatus.APPROVED, moderatorId);
    }

    public void decline(UUID moderatorId) {
        decide(JoinRequestStatus.DECLINED, moderatorId);
    }

    private void decide(JoinRequestStatus outcome, UUID moderatorId) {
        this.status = outcome;
        this.decidedBy = moderatorId;
        this.decidedAt = Instant.now();
    }
}
