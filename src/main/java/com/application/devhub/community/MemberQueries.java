package com.application.devhub.community;

import com.application.devhub.common.pagination.KeysetCursor;
import com.application.devhub.common.visibility.VisibilitySql;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MemberQueries {

    static final int PAGE_SIZE = 30;

    private static final String MEMBERS = """
            SELECT u.id, u.username, u.display_name, m.role, m.joined_at
            FROM community_members m
            JOIN users u ON u.id = m.user_id
            WHERE m.community_id = :communityId
              AND (u.id = :viewer OR %s)
              %s
              %s
            ORDER BY m.joined_at, u.id
            LIMIT :limit
            """;

    private static final String REQUESTS = """
            SELECT r.id, r.message, r.created_at, u.id AS user_id, u.username, u.display_name
            FROM community_join_requests r
            JOIN users u ON u.id = r.user_id
            WHERE r.community_id = :communityId AND r.status = 'PENDING'
              AND %s
              %s
            ORDER BY r.created_at, r.id
            LIMIT :limit
            """;

    private static final String AFTER_MEMBER = "AND (m.joined_at, u.id) > (CAST(:cursorAt AS timestamptz), CAST(:cursorId AS uuid))";
    private static final String AFTER_REQUEST = "AND (r.created_at, r.id) > (CAST(:cursorAt AS timestamptz), CAST(:cursorId AS uuid))";

    private final JdbcClient jdbcClient;

    @Transactional(readOnly = true)
    public MemberPage members(UUID viewerId, UUID communityId, CommunityRole role, String cursor) {
        KeysetCursor after = KeysetCursor.decode(cursor);
        String sql = MEMBERS.formatted(VisibilitySql.visibleUser("viewer", "u"),
                role == null ? "" : "AND m.role = :role", after == null ? "" : AFTER_MEMBER);
        Map<String, Object> params = params(communityId, after);
        params.put("viewer", viewerId);
        if (role != null) {
            params.put("role", role.name());
        }
        List<MemberView> rows = jdbcClient.sql(sql).params(params)
                .query((row, index) -> new MemberView(row.getObject("id", UUID.class), row.getString("username"),
                        row.getString("display_name"), CommunityRole.valueOf(row.getString("role")),
                        row.getTimestamp("joined_at").toInstant()))
                .list();
        if (rows.size() <= PAGE_SIZE) {
            return new MemberPage(rows, null);
        }
        List<MemberView> items = rows.subList(0, PAGE_SIZE);
        MemberView last = items.getLast();
        return new MemberPage(items, new KeysetCursor(last.joinedAt(), last.id()).encode());
    }

    @Transactional(readOnly = true)
    public JoinRequestPage requests(UUID communityId, String cursor) {
        KeysetCursor after = KeysetCursor.decode(cursor);
        String sql = REQUESTS.formatted(VisibilitySql.activeAccount("u"), after == null ? "" : AFTER_REQUEST);
        List<JoinRequestView> rows = jdbcClient.sql(sql).params(params(communityId, after))
                .query((row, index) -> new JoinRequestView(row.getObject("id", UUID.class),
                        new JoinRequestView.Requester(row.getObject("user_id", UUID.class), row.getString("username"),
                                row.getString("display_name")),
                        row.getString("message"), row.getTimestamp("created_at").toInstant()))
                .list();
        if (rows.size() <= PAGE_SIZE) {
            return new JoinRequestPage(rows, null);
        }
        List<JoinRequestView> items = rows.subList(0, PAGE_SIZE);
        JoinRequestView last = items.getLast();
        return new JoinRequestPage(items, new KeysetCursor(last.createdAt(), last.id()).encode());
    }

    private static Map<String, Object> params(UUID communityId, KeysetCursor after) {
        Map<String, Object> params = new HashMap<>();
        params.put("communityId", communityId);
        params.put("limit", PAGE_SIZE + 1);
        if (after != null) {
            params.put("cursorAt", after.timestamp());
            params.put("cursorId", after.id());
        }
        return params;
    }

    public record MemberView(UUID id, String username, String displayName, CommunityRole role, Instant joinedAt) {
    }

    public record MemberPage(List<MemberView> items, String nextCursor) {
    }

    public record JoinRequestView(UUID id, Requester requester, String message, Instant createdAt) {

        public record Requester(UUID id, String username, String displayName) {
        }
    }

    public record JoinRequestPage(List<JoinRequestView> items, String nextCursor) {
    }
}
