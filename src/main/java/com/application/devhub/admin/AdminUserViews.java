package com.application.devhub.admin;

import com.application.devhub.moderation.ModerationViews.CaseView;
import com.application.devhub.user.Role;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AdminUserViews {

    private AdminUserViews() {
    }

    public record UserSummary(UUID id, String username, String displayName, String email, Role role,
                              AccountStatus status, Instant createdAt) {
    }

    public record UserDetail(UserSummary user, Instant emailVerifiedAt, Instant suspendedUntil, Instant bannedAt,
                             Instant deactivatedAt, Instant deletedAt, long postCount, long reportsFiled,
                             long reportsAgainst, List<CaseView> cases) {
    }
}
