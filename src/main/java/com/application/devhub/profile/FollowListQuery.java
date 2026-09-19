package com.application.devhub.profile;

import com.application.devhub.common.pagination.KeysetCursor;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FollowListQuery {

    public static final int PAGE_SIZE = 20;

    private static final String SQL = """
            SELECT u.id, u.username, u.display_name, f.created_at,
                   EXISTS (SELECT 1 FROM follows v WHERE v.follower_id = :viewer AND v.followee_id = u.id) AS following
            FROM follows f
            JOIN users u ON u.id = f.%1$s
            WHERE f.%2$s = :target
              AND (CAST(:cursorAt AS timestamptz) IS NULL
                   OR (f.created_at, u.id) < (CAST(:cursorAt AS timestamptz), CAST(:cursorId AS uuid)))
            ORDER BY f.created_at DESC, u.id DESC
            LIMIT :limit
            """;

    private final JdbcClient jdbcClient;

    @Transactional(readOnly = true)
    public PersonPage followers(UUID viewerId, UUID targetId, String cursor) {
        return page(SQL.formatted("follower_id", "followee_id"), viewerId, targetId, cursor);
    }

    @Transactional(readOnly = true)
    public PersonPage following(UUID viewerId, UUID targetId, String cursor) {
        return page(SQL.formatted("followee_id", "follower_id"), viewerId, targetId, cursor);
    }

    private PersonPage page(String sql, UUID viewerId, UUID targetId, String cursor) {
        KeysetCursor position = KeysetCursor.decode(cursor);
        List<Row> rows = jdbcClient.sql(sql)
                .param("viewer", viewerId)
                .param("target", targetId)
                .param("cursorAt", position == null ? null : position.timestamp())
                .param("cursorId", position == null ? null : position.id())
                .param("limit", PAGE_SIZE + 1)
                .query((row, index) -> Row.of(row))
                .list();
        boolean more = rows.size() > PAGE_SIZE;
        List<Row> visible = more ? rows.subList(0, PAGE_SIZE) : rows;
        Row last = visible.isEmpty() ? null : visible.getLast();
        return new PersonPage(visible.stream().map(Row::person).toList(),
                more ? new KeysetCursor(last.followedAt(), last.person().id()).encode() : null);
    }

    private record Row(PersonSummary person, Instant followedAt) {

        static Row of(ResultSet row) throws SQLException {
            return new Row(new PersonSummary(row.getObject("id", UUID.class), row.getString("username"),
                    row.getString("display_name"), row.getBoolean("following")),
                    row.getTimestamp("created_at").toInstant());
        }
    }
}
