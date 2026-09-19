package com.application.devhub.moderation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "case_id", nullable = false, updatable = false)
    private UUID caseId;

    @Column(name = "reporter_id", nullable = false, updatable = false)
    private UUID reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 10, updatable = false)
    private ReportTarget targetType;

    @Column(name = "target_id", nullable = false, updatable = false)
    private UUID targetId;

    @Column(name = "target_owner_id", nullable = false, updatable = false)
    private UUID targetOwnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private ReportReason reason;

    @Column(length = 500, updatable = false)
    private String note;

    @Column(nullable = false, updatable = false)
    private String snapshot;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private Report(UUID caseId, UUID reporterId, ReportTarget targetType, UUID targetId, UUID targetOwnerId,
                   ReportReason reason, String note, String snapshot) {
        this.caseId = caseId;
        this.reporterId = reporterId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetOwnerId = targetOwnerId;
        this.reason = reason;
        this.note = note;
        this.snapshot = snapshot;
    }

    public static Report of(UUID caseId, UUID reporterId, ReportTarget targetType, UUID targetId, UUID targetOwnerId,
                            ReportReason reason, String note, String snapshot) {
        return new Report(caseId, reporterId, targetType, targetId, targetOwnerId, reason, note, snapshot);
    }
}
