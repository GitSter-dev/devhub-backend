package com.application.devhub.post;

import com.application.devhub.common.api.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ThreadQuery {

    private static final String ANCESTORS = """
            WITH RECURSIVE chain (id, parent_id) AS (
                SELECT p.id, p.parent_id FROM posts p WHERE p.id = (SELECT parent_id FROM posts WHERE id = :postId)
                UNION ALL
                SELECT p.id, p.parent_id FROM posts p JOIN chain c ON p.id = c.parent_id
            )
            SELECT id FROM chain
            """;
    private static final String VISIBLE_REPLIES = """
            p.parent_id = :postId
            AND (p.deleted_at IS NULL OR EXISTS (SELECT 1 FROM posts child WHERE child.parent_id = p.id))""";
    private static final String USER_POSTS = "p.author_id = :authorId AND p.parent_id IS NULL AND p.deleted_at IS NULL";

    private final PostViews postViews;
    private final JdbcClient jdbcClient;

    @Transactional(readOnly = true)
    public ThreadResponse thread(UUID viewerId, UUID postId) {
        PostView post = postViews.byId(viewerId, postId).orElseThrow(ApiException::notFound);
        List<UUID> ancestorIds = jdbcClient.sql(ANCESTORS).param("postId", postId).query(UUID.class).list();
        return new ThreadResponse(post, postViews.byIds(viewerId, ancestorIds));
    }

    @Transactional(readOnly = true)
    public PostPage replies(UUID viewerId, UUID postId, String cursor) {
        postViews.byId(viewerId, postId).orElseThrow(ApiException::notFound);
        return postViews.page(viewerId, VISIBLE_REPLIES, Map.of("postId", postId), cursor, true);
    }

    @Transactional(readOnly = true)
    public PostPage byAuthor(UUID viewerId, UUID authorId, String cursor) {
        return postViews.page(viewerId, USER_POSTS, Map.of("authorId", authorId), cursor, false);
    }
}
