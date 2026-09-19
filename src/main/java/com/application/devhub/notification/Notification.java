package com.application.devhub.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    private UUID id;

    @Column(name = "recipient_id", nullable = false, updatable = false)
    private UUID recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private NotificationType type;

    @Column(name = "group_key", nullable = false, length = 80, updatable = false)
    private String groupKey;

    @Column(name = "subject_id", updatable = false)
    private UUID subjectId;

    @Column(name = "seen_at")
    private Instant seenAt;

    @Column(name = "push_due_at")
    private Instant pushDueAt;

    @Column(name = "pushed_count", nullable = false)
    private int pushedCount;

    @Column(name = "pushed_at")
    private Instant pushedAt;

    public void markPushed() {
        this.pushedCount += 1;
        this.pushedAt = Instant.now();
        this.pushDueAt = null;
    }

    public void skipPush() {
        this.pushDueAt = null;
    }
}
