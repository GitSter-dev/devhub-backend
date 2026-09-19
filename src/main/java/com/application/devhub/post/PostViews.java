package com.application.devhub.post;

import com.application.devhub.common.pagination.KeysetCursor;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostViews {

    public static final int PAGE_SIZE = 20;

    private static final String SELECT = """
            SELECT p.id, p.body, p.code, p.code_language, p.created_at, p.parent_id, p.root_id,
                   p.deleted_at IS NOT NULL AS deleted,
                   a.id AS author_id, a.username AS author_username, a.display_name AS author_display_name,
                   parent_author.username AS reply_to_username,
                   parent.deleted_at IS NOT NULL AS reply_to_deleted,
                   (SELECT count(*) FROM posts r WHERE r.parent_id = p.id AND r.deleted_at IS NULL) AS reply_count,
                   (SELECT count(*) FROM post_likes l WHERE l.post_id = p.id) AS like_count,
                   EXISTS (SELECT 1 FROM post_likes l WHERE l.post_id = p.id AND l.user_id = :viewer) AS liked
            FROM posts p
            JOIN users a ON a.id = p.author_id
            LEFT JOIN posts parent ON parent.id = p.parent_id
            LEFT JOIN users parent_author ON parent_author.id = parent.author_id
            """;

    private static final String AFTER = "(p.created_at, p.id) > (CAST(:cursorAt AS timestamptz), CAST(:cursorId AS uuid))";
    private static final String BEFORE = "(p.created_at, p.id) < (CAST(:cursorAt AS timestamptz), CAST(:cursorId AS uuid))";

    private final JdbcClient jdbcClient;

    public Optional<PostView> byId(UUID viewerId, UUID postId) {
        return jdbcClient.sql(SELECT + " WHERE p.id = :postId")
                .param("viewer", viewerId)
                .param("postId", postId)
                .query((row, index) -> toView(row, viewerId))
                .optional();
    }

    public List<PostView> byIds(UUID viewerId, List<UUID> postIds) {
        if (postIds.isEmpty()) {
            return List.of();
        }
        return jdbcClient.sql(SELECT + " WHERE p.id IN (:postIds) ORDER BY p.created_at, p.id")
                .param("viewer", viewerId)
                .param("postIds", postIds)
                .query((row, index) -> toView(row, viewerId))
                .list();
    }

    public List<PostView> slice(UUID viewerId, String where, Map<String, Object> params, KeysetCursor cursor,
                                boolean oldestFirst, int limit) {
        String keyset = cursor == null ? "" : " AND " + (oldestFirst ? AFTER : BEFORE);
        String order = oldestFirst ? "p.created_at ASC, p.id ASC" : "p.created_at DESC, p.id DESC";
        Map<String, Object> all = new HashMap<>(params);
        all.put("viewer", viewerId);
        all.put("limit", limit);
        if (cursor != null) {
            all.put("cursorAt", cursor.timestamp());
            all.put("cursorId", cursor.id());
        }
        return jdbcClient.sql(SELECT + " WHERE " + where + keyset + " ORDER BY " + order + " LIMIT :limit")
                .params(all)
                .query((row, index) -> toView(row, viewerId))
                .list();
    }

    public PostPage page(UUID viewerId, String where, Map<String, Object> params, String cursor, boolean oldestFirst) {
        List<PostView> rows = slice(viewerId, where, params, KeysetCursor.decode(cursor), oldestFirst, PAGE_SIZE + 1);
        if (rows.size() <= PAGE_SIZE) {
            return new PostPage(rows, null);
        }
        List<PostView> items = rows.subList(0, PAGE_SIZE);
        return new PostPage(items, cursorAfter(items.getLast()).encode());
    }

    public static KeysetCursor cursorAfter(PostView post) {
        return new KeysetCursor(post.createdAt(), post.id());
    }

    private static PostView toView(ResultSet row, UUID viewerId) throws SQLException {
        UUID authorId = row.getObject("author_id", UUID.class);
        return new PostView(
                row.getObject("id", UUID.class),
                new PostView.Author(authorId, row.getString("author_username"), row.getString("author_display_name")),
                row.getString("body"),
                row.getString("code"),
                row.getString("code_language"),
                row.getTimestamp("created_at").toInstant(),
                row.getObject("parent_id", UUID.class),
                row.getString("reply_to_username"),
                row.getBoolean("reply_to_deleted"),
                row.getObject("root_id", UUID.class),
                row.getLong("reply_count"),
                row.getLong("like_count"),
                row.getBoolean("liked"),
                authorId.equals(viewerId),
                row.getBoolean("deleted"));
    }
}
