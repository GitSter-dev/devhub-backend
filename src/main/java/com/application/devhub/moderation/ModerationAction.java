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
@Table(name = "moderation_actions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModerationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "case_id", updatable = false)
    private UUID caseId;

    @Column(name = "moderator_id", nullable = false, updatable = false)
    private UUID moderatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private ModerationActionType action;

    @Column(name = "target_user_id", updatable = false)
    private UUID targetUserId;

    @Column(length = 500, updatable = false)
    private String note;

    @Column(name = "acts_until", updatable = false)
    private Instant actsUntil;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private ModerationAction(UUID caseId, UUID moderatorId, ModerationActionType action, UUID targetUserId,
                             String note, Instant actsUntil) {
        this.caseId = caseId;
        this.moderatorId = moderatorId;
        this.action = action;
        this.targetUserId = targetUserId;
        this.note = note;
        this.actsUntil = actsUntil;
    }

    public static ModerationAction of(UUID caseId, UUID moderatorId, ModerationActionType action, UUID targetUserId,
                                      String note, Instant actsUntil) {
        return new ModerationAction(caseId, moderatorId, action, targetUserId, note, actsUntil);
    }
}
