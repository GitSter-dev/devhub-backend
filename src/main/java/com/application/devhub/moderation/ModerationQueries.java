package com.application.devhub.moderation;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.moderation.ModerationViews.ActionView;
import com.application.devhub.moderation.ModerationViews.CaseDetail;
import com.application.devhub.moderation.ModerationViews.CasePage;
import com.application.devhub.moderation.ModerationViews.CaseView;
import com.application.devhub.moderation.ModerationViews.ReportView;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ModerationQueries {

    private static final String CASES = """
            SELECT c.id, c.target_type, c.target_id, c.owner_id, o.username AS owner_username, c.status,
                   c.reporter_count, c.severity, c.auto_hidden_at IS NOT NULL AS auto_hidden,
                   c.first_reported_at, c.last_reported_at
            FROM moderation_cases c
            JOIN users o ON o.id = c.owner_id
            """;

    private final JdbcClient jdbcClient;
    private final ModerationProperties properties;

    @Transactional(readOnly = true)
    public CasePage cases(CaseStatus status, String cursor) {
        CaseCursor position = CaseCursor.decode(cursor);
        String keyset = position == null ? ""
                : " AND (c.severity, c.reporter_count, c.last_reported_at, c.id)"
                + " < (:cursorSeverity, :cursorReporters, CAST(:cursorAt AS timestamptz), CAST(:cursorId AS uuid))";
        var query = jdbcClient.sql(CASES + " WHERE c.status = :status" + keyset
                        + " ORDER BY c.severity DESC, c.reporter_count DESC, c.last_reported_at DESC, c.id DESC"
                        + " LIMIT :limit")
                .param("status", status.name())
                .param("limit", properties.casePageSize() + 1);
        if (position != null) {
            query = query.param("cursorSeverity", position.severity())
                    .param("cursorReporters", position.reporterCount())
                    .param("cursorAt", position.lastReportedTimestamp())
                    .param("cursorId", position.id());
        }
        List<CaseView> rows = query.query((row, index) -> toCase(row)).list();
        boolean more = rows.size() > properties.casePageSize();
        List<CaseView> page = more ? rows.subList(0, properties.casePageSize()) : rows;
        return new CasePage(page, more ? CaseCursor.after(page.getLast()).encode() : null);
    }

    @Transactional(readOnly = true)
    public List<CaseView> casesOwnedBy(UUID ownerId, int limit) {
        return jdbcClient.sql(CASES + " WHERE c.owner_id = :ownerId ORDER BY c.last_reported_at DESC, c.id DESC"
                        + " LIMIT :limit")
                .param("ownerId", ownerId)
                .param("limit", limit)
                .query((row, index) -> toCase(row))
                .list();
    }

    @Transactional(readOnly = true)
    public CaseDetail caseDetail(UUID caseId) {
        CaseView moderationCase = jdbcClient.sql(CASES + " WHERE c.id = :caseId")
                .param("caseId", caseId)
                .query((row, index) -> toCase(row))
                .optional()
                .orElseThrow(ApiException::notFound);
        List<ReportView> reports = jdbcClient.sql("""
                        SELECT r.id, u.username AS reporter_username, r.reason, r.note, r.snapshot, r.created_at
                        FROM reports r
                        JOIN users u ON u.id = r.reporter_id
                        WHERE r.case_id = :caseId
                        ORDER BY r.created_at DESC
                        """)
                .param("caseId", caseId)
                .query((row, index) -> new ReportView(row.getObject("id", UUID.class),
                        row.getString("reporter_username"), ReportReason.valueOf(row.getString("reason")),
                        row.getString("note"), row.getString("snapshot"), row.getTimestamp("created_at").toInstant()))
                .list();
        List<ActionView> actions = jdbcClient.sql("""
                        SELECT m.username AS moderator_username, a.action, a.note, a.acts_until, a.created_at
                        FROM moderation_actions a
                        JOIN users m ON m.id = a.moderator_id
                        WHERE a.case_id = :caseId
                        ORDER BY a.created_at DESC
                        """)
                .param("caseId", caseId)
                .query((row, index) -> new ActionView(row.getString("moderator_username"),
                        ModerationActionType.valueOf(row.getString("action")), row.getString("note"),
                        row.getTimestamp("acts_until") == null ? null : row.getTimestamp("acts_until").toInstant(),
                        row.getTimestamp("created_at").toInstant()))
                .list();
        return new CaseDetail(moderationCase, reports, actions);
    }

    private static CaseView toCase(ResultSet row) throws SQLException {
        return new CaseView(
                row.getObject("id", UUID.class),
                ReportTarget.valueOf(row.getString("target_type")),
                row.getObject("target_id", UUID.class),
                row.getObject("owner_id", UUID.class),
                row.getString("owner_username"),
                CaseStatus.valueOf(row.getString("status")),
                row.getInt("reporter_count"),
                row.getInt("severity"),
                row.getBoolean("auto_hidden"),
                row.getTimestamp("first_reported_at").toInstant(),
                row.getTimestamp("last_reported_at").toInstant());
    }
}
