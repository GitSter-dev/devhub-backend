package com.application.devhub.otp;

import com.application.devhub.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "one_time_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OneTimeCode extends BaseEntity {

    static final Duration ISSUE_WINDOW = Duration.ofDays(1);

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OneTimeCodePurpose purpose;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "window_started_at", nullable = false)
    private Instant windowStartedAt;

    @Column(name = "issues_in_window", nullable = false)
    private int issuesInWindow;

    public OneTimeCode(UUID userId, OneTimeCodePurpose purpose) {
        this.userId = userId;
        this.purpose = purpose;
    }

    public void reissue(String codeHash, Instant expiresAt, Instant now) {
        if (windowStartedAt == null || !windowStartedAt.plus(ISSUE_WINDOW).isAfter(now)) {
            this.windowStartedAt = now;
            this.issuesInWindow = 0;
        }
        this.issuesInWindow++;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.attempts = 0;
        this.issuedAt = now;
    }

    public boolean isActive() {
        return Instant.now().isBefore(expiresAt);
    }

    public boolean wasIssuedAfter(Instant instant) {
        return issuedAt.isAfter(instant);
    }

    public int issuesInCurrentWindow(Instant now) {
        return windowStartedAt.plus(ISSUE_WINDOW).isAfter(now) ? issuesInWindow : 0;
    }

    public int registerFailedAttempt() {
        return ++attempts;
    }

    public void invalidate() {
        this.expiresAt = Instant.now();
    }
}
