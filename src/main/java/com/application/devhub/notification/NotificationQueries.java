package com.application.devhub.notification;

import com.application.devhub.common.pagination.KeysetCursor;
import com.application.devhub.notification.NotificationViews.Actor;
import com.application.devhub.notification.NotificationViews.NotificationPage;
import com.application.devhub.notification.NotificationViews.NotificationView;
import com.application.devhub.notification.NotificationViews.UnseenCount;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationQueries {

    public static final int SHOWN_ACTORS = 3;
    private static final int PREVIEW_LENGTH = 140;

    private static final String LIVE_SUBJECT = """
            NOT EXISTS (SELECT 1 FROM posts gone WHERE gone.id = n.subject_id AND gone.deleted_at IS NOT NULL)
            """;

    private static final String NOTIFICATIONS = """
            SELECT n.id, n.type, n.subject_id, n.updated_at, n.seen_at, latest.subject_id AS target_id,
                   (SELECT count(*) FROM notification_actors c WHERE c.notification_id = n.id) AS actor_count,
                   LEFT(preview.body, %d) AS preview_body,
                   preview.code IS NOT NULL AS preview_has_code
            FROM notifications n
            JOIN LATERAL (
                SELECT a.subject_id FROM notification_actors a
                WHERE a.notification_id = n.id
                ORDER BY a.acted_at DESC
                LIMIT 1
            ) latest ON true
            LEFT JOIN posts preview ON preview.deleted_at IS NULL AND preview.id = CASE
                WHEN n.type = 'POST_LIKED' THEN n.subject_id
                WHEN n.type IN ('POST_REPLIED', 'FOLLOWED_POSTED') THEN latest.subject_id
            END
            WHERE %s
            """.formatted(PREVIEW_LENGTH, LIVE_SUBJECT);

    private static final String ACTORS = """
            SELECT ranked.notification_id, u.id, u.username, u.display_name
            FROM (
                SELECT a.notification_id, a.actor_id, a.acted_at,
                       row_number() OVER (PARTITION BY a.notification_id ORDER BY a.acted_at DESC) AS position
                FROM notification_actors a
                WHERE a.notification_id IN (:ids)
            ) ranked
            JOIN users u ON u.id = ranked.actor_id
            WHERE ranked.position <= :shown
            ORDER BY ranked.notification_id, ranked.acted_at DESC
            """;

    private final JdbcClient jdbcClient;
    private final NotificationProperties properties;

    @Transactional(readOnly = true)
    public NotificationPage page(UUID viewerId, String cursor) {
        KeysetCursor position = KeysetCursor.decode(cursor);
        String keyset = position == null ? ""
                : " AND (n.updated_at, n.id) < (CAST(:cursorAt AS timestamptz), CAST(:cursorId AS uuid))";
        var query = jdbcClient.sql(NOTIFICATIONS + " AND n.recipient_id = :viewer" + keyset
                        + " ORDER BY n.updated_at DESC, n.id DESC LIMIT :limit")
                .param("viewer", viewerId)
                .param("limit", properties.pageSize() + 1);
        if (position != null) {
            query = query.param("cursorAt", position.timestamp()).param("cursorId", position.id());
        }
        List<Row> rows = query.query((result, index) -> Row.of(result)).list();
        boolean more = rows.size() > properties.pageSize();
        List<Row> page = more ? rows.subList(0, properties.pageSize()) : rows;
        String next = more ? new KeysetCursor(page.getLast().updatedAt(), page.getLast().id()).encode() : null;
        return new NotificationPage(assemble(page), next);
    }

    @Transactional(readOnly = true)
    public Optional<NotificationView> find(UUID notificationId) {
        List<Row> rows = jdbcClient.sql(NOTIFICATIONS + " AND n.id = :id")
                .param("id", notificationId)
                .query((result, index) -> Row.of(result))
                .list();
        return assemble(rows).stream().findFirst();
    }

    @Transactional(readOnly = true)
    public UnseenCount unseenCount(UUID viewerId) {
        long count = jdbcClient.sql("""
                        SELECT count(*) FROM notifications n
                        WHERE n.recipient_id = :viewer
                          AND n.seen_at IS NULL
                          AND EXISTS (SELECT 1 FROM notification_actors a WHERE a.notification_id = n.id)
                          AND\s""" + LIVE_SUBJECT)
                .param("viewer", viewerId)
                .query(Long.class)
                .single();
        return new UnseenCount(count);
    }

    private List<NotificationView> assemble(List<Row> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<Actor>> actors = new LinkedHashMap<>();
        jdbcClient.sql(ACTORS)
                .param("ids", rows.stream().map(Row::id).toList())
                .param("shown", SHOWN_ACTORS)
                .query((result, index) -> {
                    actors.computeIfAbsent(result.getObject("notification_id", UUID.class), id -> new ArrayList<>())
                            .add(new Actor(result.getObject("id", UUID.class), result.getString("username"),
                                    result.getString("display_name")));
                    return null;
                })
                .list();
        return rows.stream().map(row -> row.view(actors.getOrDefault(row.id(), List.of()))).toList();
    }

    private record Row(UUID id, NotificationType type, UUID subjectId, UUID targetId, int actorCount, String preview,
                       boolean previewHasCode, Instant updatedAt, boolean seen) {

        static Row of(ResultSet result) throws SQLException {
            return new Row(
                    result.getObject("id", UUID.class),
                    NotificationType.valueOf(result.getString("type")),
                    result.getObject("subject_id", UUID.class),
                    result.getObject("target_id", UUID.class),
                    result.getInt("actor_count"),
                    result.getString("preview_body"),
                    result.getBoolean("preview_has_code"),
                    result.getTimestamp("updated_at").toInstant(),
                    result.getTimestamp("seen_at") != null);
        }

        NotificationView view(List<Actor> actors) {
            return new NotificationView(id, type, actors, actorCount, subjectId, targetId, preview, previewHasCode,
                    updatedAt, seen);
        }
    }
}
