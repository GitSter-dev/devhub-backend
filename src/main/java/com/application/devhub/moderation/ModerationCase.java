package com.application.devhub.moderation;

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
@Table(name = "moderation_cases")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModerationCase extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 10, updatable = false)
    private ReportTarget targetType;

    @Column(name = "target_id", nullable = false, updatable = false)
    private UUID targetId;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "community_id", updatable = false)
    private UUID communityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private CaseStatus status;

    @Column(name = "reporter_count", nullable = false)
    private int reporterCount;

    @Column(nullable = false)
    private int severity;

    @Column(name = "auto_hidden_at")
    private Instant autoHiddenAt;

    @Column(name = "first_reported_at", nullable = false)
    private Instant firstReportedAt;

    @Column(name = "last_reported_at", nullable = false)
    private Instant lastReportedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    private ModerationCase(ReportTarget targetType, UUID targetId, UUID ownerId, UUID communityId) {
        this.communityId = communityId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.ownerId = ownerId;
        this.status = CaseStatus.OPEN;
        this.firstReportedAt = Instant.now();
        this.lastReportedAt = this.firstReportedAt;
    }

    public static ModerationCase open(ReportTarget targetType, UUID targetId, UUID ownerId, UUID communityId) {
        return new ModerationCase(targetType, targetId, ownerId, communityId);
    }

    public void recordReport(int weight) {
        this.reporterCount += 1;
        this.severity = Math.max(severity, weight);
        this.lastReportedAt = Instant.now();
        if (status == CaseStatus.DISMISSED) {
            this.status = CaseStatus.OPEN;
            this.resolvedAt = null;
            this.resolvedBy = null;
        }
    }

    public boolean isAutoHidden() {
        return autoHiddenAt != null;
    }

    public void autoHide() {
        if (autoHiddenAt == null) {
            this.autoHiddenAt = Instant.now();
        }
    }

    public void clearAutoHide() {
        this.autoHiddenAt = null;
    }

    public void resolve(UUID moderatorId, CaseStatus outcome) {
        this.status = outcome;
        this.resolvedBy = moderatorId;
        this.resolvedAt = Instant.now();
    }
}
