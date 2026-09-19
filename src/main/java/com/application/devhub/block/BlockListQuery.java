package com.application.devhub.block;

import com.application.devhub.common.pagination.KeysetCursor;
import com.application.devhub.profile.PersonPage;
import com.application.devhub.profile.PersonSummary;
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
public class BlockListQuery {

    private static final int PAGE_SIZE = 30;

    private static final String SQL = """
            SELECT u.id, u.username, u.display_name, b.created_at
            FROM blocks b
            JOIN users u ON u.id = b.blocked_id
            WHERE b.blocker_id = :me
              AND (CAST(:cursorAt AS timestamptz) IS NULL
                   OR (b.created_at, u.id) < (CAST(:cursorAt AS timestamptz), CAST(:cursorId AS uuid)))
            ORDER BY b.created_at DESC, u.id DESC
            LIMIT :limit
            """;

    private final JdbcClient jdbcClient;

    @Transactional(readOnly = true)
    public PersonPage blocked(UUID me, String cursor) {
        KeysetCursor position = KeysetCursor.decode(cursor);
        List<Row> rows = jdbcClient.sql(SQL)
                .param("me", me)
                .param("cursorAt", position == null ? null : position.timestamp())
                .param("cursorId", position == null ? null : position.id())
                .param("limit", PAGE_SIZE + 1)
                .query((row, index) -> Row.of(row))
                .list();
        boolean more = rows.size() > PAGE_SIZE;
        List<Row> page = more ? rows.subList(0, PAGE_SIZE) : rows;
        return new PersonPage(page.stream().map(Row::person).toList(),
                more ? new KeysetCursor(page.getLast().blockedAt(), page.getLast().person().id()).encode() : null);
    }

    private record Row(PersonSummary person, Instant blockedAt) {

        static Row of(ResultSet row) throws SQLException {
            return new Row(new PersonSummary(row.getObject("id", UUID.class), row.getString("username"),
                    row.getString("display_name"), false), row.getTimestamp("created_at").toInstant());
        }
    }
}
