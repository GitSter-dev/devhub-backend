package com.application.devhub.admin;

import java.time.Instant;

public enum AccountStatus {
    ACTIVE,
    UNVERIFIED,
    SUSPENDED,
    BANNED,
    DEACTIVATED,
    DELETED;

    static AccountStatus of(Instant emailVerifiedAt, Instant suspendedUntil, Instant bannedAt, Instant deactivatedAt,
                            Instant deletedAt, Instant now) {
        if (deletedAt != null) {
            return DELETED;
        }
        if (bannedAt != null) {
            return BANNED;
        }
        if (suspendedUntil != null && suspendedUntil.isAfter(now)) {
            return SUSPENDED;
        }
        if (deactivatedAt != null) {
            return DEACTIVATED;
        }
        return emailVerifiedAt == null ? UNVERIFIED : ACTIVE;
    }
}
