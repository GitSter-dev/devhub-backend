package com.application.devhub.outbox;

import com.application.devhub.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OutboxEventType type;

    @ColumnTransformer(write = "?::jsonb")
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "processed_at")
    private Instant processedAt;

    public OutboxEvent(OutboxEventType type, String payload) {
        this.type = type;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.nextAttemptAt = Instant.now();
    }

    public void markSent() {
        this.status = OutboxStatus.SENT;
        this.processedAt = Instant.now();
        this.lastError = null;
    }

    public void recordFailure(String error, int maxAttempts, Duration backoff) {
        this.attempts++;
        this.lastError = error;
        if (attempts >= maxAttempts) {
            this.status = OutboxStatus.FAILED;
            this.processedAt = Instant.now();
            return;
        }
        this.nextAttemptAt = Instant.now().plus(backoff.multipliedBy(1L << (attempts - 1)));
    }
}
