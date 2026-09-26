package com.application.devhub.moderation;

import com.application.devhub.common.pagination.KeysetCursor;
import com.application.devhub.moderation.ModerationViews.AuditEntry;
import com.application.devhub.moderation.ModerationViews.AuditPage;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuditLogQuery {

    private static final int PAGE_SIZE = 50;

    private static final String ENTRIES = """
            SELECT a.id, a.case_id, m.username AS moderator_username, a.action, a.target_user_id,
                   t.username AS target_username, a.note, a.acts_until, a.created_at
            FROM moderation_actions a
            JOIN users m ON m.id = a.moderator_id
            LEFT JOIN users t ON t.id = a.target_user_id
            WHERE TRUE
            """;

    private final JdbcClient jdbcClient;

    @Transactional(readOnly = true)
    public AuditPage entries(UUID targetUserId, String cursor) {
        KeysetCursor position = KeysetCursor.decode(cursor);
        String sql = ENTRIES
                + (targetUserId == null ? "" : " AND a.target_user_id = :targetUserId")
                + (position == null ? ""
                : " AND (a.created_at, a.id) < (CAST(:cursorAt AS timestamptz), CAST(:cursorId AS uuid))")
                + " ORDER BY a.created_at DESC, a.id DESC LIMIT :limit";
        var query = jdbcClient.sql(sql).param("limit", PAGE_SIZE + 1);
        if (targetUserId != null) {
            query = query.param("targetUserId", targetUserId);
        }
        if (position != null) {
            query = query.param("cursorAt", position.timestamp()).param("cursorId", position.id());
        }
        List<AuditEntry> rows = query.query((row, index) -> toEntry(row)).list();
        boolean more = rows.size() > PAGE_SIZE;
        List<AuditEntry> page = more ? rows.subList(0, PAGE_SIZE) : rows;
        return new AuditPage(page, more
                ? new KeysetCursor(page.getLast().createdAt(), page.getLast().id()).encode() : null);
    }

    private static AuditEntry toEntry(ResultSet row) throws SQLException {
        Timestamp actsUntil = row.getTimestamp("acts_until");
        return new AuditEntry(
                row.getObject("id", UUID.class),
                row.getObject("case_id", UUID.class),
                row.getString("moderator_username"),
                ModerationActionType.valueOf(row.getString("action")),
                row.getObject("target_user_id", UUID.class),
                row.getString("target_username"),
                row.getString("note"),
                actsUntil == null ? null : actsUntil.toInstant(),
                row.getTimestamp("created_at").toInstant());
    }
}
