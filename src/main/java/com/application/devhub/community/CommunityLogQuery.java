package com.application.devhub.community;

import com.application.devhub.common.pagination.KeysetCursor;
import com.application.devhub.moderation.ModerationActionType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CommunityLogQuery {

    static final int PAGE_SIZE = 30;

    private static final String ENTRIES = """
            SELECT a.id, a.action, a.note, a.acts_until, a.created_at, a.target_post_id,
                   m.username AS moderator_username, t.username AS target_username
            FROM moderation_actions a
            JOIN users m ON m.id = a.moderator_id
            LEFT JOIN users t ON t.id = a.target_user_id
            WHERE a.community_id = :communityId
              AND a.action IN ('REMOVE_CONTENT', 'RESTORE', 'COMMUNITY_BAN', 'COMMUNITY_UNBAN')
            """;

    private static final String BANS = """
            SELECT u.id, u.username, u.display_name, b.reason, b.expires_at, b.created_at, m.username AS banned_by
            FROM community_bans b
            JOIN users u ON u.id = b.user_id
            JOIN users m ON m.id = b.banned_by
            WHERE b.community_id = :communityId
              AND (b.expires_at IS NULL OR b.expires_at > now())
            ORDER BY b.created_at DESC, u.id
            """;

    private final JdbcClient jdbcClient;

    @Transactional(readOnly = true)
    public LogPage entries(UUID communityId, String cursor) {
        KeysetCursor position = KeysetCursor.decode(cursor);
        String keyset = position == null ? ""
                : " AND (a.created_at, a.id) < (CAST(:cursorAt AS timestamptz), CAST(:cursorId AS uuid))";
        var query = jdbcClient.sql(ENTRIES + keyset + " ORDER BY a.created_at DESC, a.id DESC LIMIT :limit")
                .param("communityId", communityId)
                .param("limit", PAGE_SIZE + 1);
        if (position != null) {
            query = query.param("cursorAt", position.timestamp()).param("cursorId", position.id());
        }
        List<LogEntry> rows = query.query((row, index) -> new LogEntry(
                        row.getObject("id", UUID.class),
                        ModerationActionType.valueOf(row.getString("action")),
                        row.getString("moderator_username"),
                        row.getString("target_username"),
                        row.getObject("target_post_id", UUID.class),
                        row.getString("note"),
                        instant(row.getTimestamp("acts_until")),
                        row.getTimestamp("created_at").toInstant()))
                .list();
        boolean more = rows.size() > PAGE_SIZE;
        List<LogEntry> page = more ? rows.subList(0, PAGE_SIZE) : rows;
        return new LogPage(page, more ? new KeysetCursor(page.getLast().createdAt(), page.getLast().id()).encode() : null);
    }

    @Transactional(readOnly = true)
    public List<BanView> bans(UUID communityId) {
        return jdbcClient.sql(BANS)
                .param("communityId", communityId)
                .query((row, index) -> new BanView(row.getObject("id", UUID.class), row.getString("username"),
                        row.getString("display_name"), row.getString("reason"), instant(row.getTimestamp("expires_at")),
                        row.getTimestamp("created_at").toInstant(), row.getString("banned_by")))
                .list();
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    public record LogEntry(UUID id, ModerationActionType action, String moderatorUsername, String targetUsername,
                           UUID postId, String note, Instant until, Instant createdAt) {
    }

    public record LogPage(List<LogEntry> items, String nextCursor) {
    }

    public record BanView(UUID userId, String username, String displayName, String reason, Instant expiresAt,
                          Instant bannedAt, String bannedBy) {
    }
}
