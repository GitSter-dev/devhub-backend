package com.application.devhub.moderation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ModerationViews {

    private ModerationViews() {
    }

    public record CaseView(UUID id, ReportTarget targetType, UUID targetId, UUID ownerId, String ownerUsername,
                           CaseStatus status, int reporterCount, int severity, boolean autoHidden,
                           Instant firstReportedAt, Instant lastReportedAt) {
    }

    public record CasePage(List<CaseView> items, String nextCursor) {
    }

    public record ReportView(UUID id, String reporterUsername, ReportReason reason, String note, String snapshot,
                             Instant createdAt) {
    }

    public record ActionView(String moderatorUsername, ModerationActionType action, String note, Instant actsUntil,
                             Instant createdAt) {
    }

    public record AuditEntry(UUID id, UUID caseId, String moderatorUsername, ModerationActionType action,
                             UUID targetUserId, String targetUsername, String note, Instant actsUntil,
                             Instant createdAt) {
    }

    public record AuditPage(List<AuditEntry> items, String nextCursor) {
    }

    public record CaseDetail(CaseView moderationCase, List<ReportView> reports, List<ActionView> actions) {
    }
}
