package com.application.devhub.community;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CommunityViews {

    private static final String SUMMARY = """
            SELECT c.id, c.slug, c.name, c.description, c.join_policy, c.member_count,
                   EXISTS (SELECT 1 FROM community_members m WHERE m.community_id = c.id AND m.user_id = :viewer) AS joined
            FROM communities c
            """;

    private static final String DETAIL = """
            SELECT c.id, c.slug, c.name, c.description, c.join_policy, c.member_count, c.slow_mode_seconds, c.created_at,
                   ARRAY(SELECT t.topic_slug FROM community_topics t WHERE t.community_id = c.id ORDER BY t.topic_slug) AS topics,
                   (SELECT m.role FROM community_members m WHERE m.community_id = c.id AND m.user_id = :viewer) AS viewer_role,
                   EXISTS (SELECT 1 FROM community_join_requests r
                           WHERE r.community_id = c.id AND r.user_id = :viewer AND r.status = 'PENDING') AS requested
            FROM communities c
            WHERE c.slug = :slug AND c.removed_at IS NULL
            """;

    private static final String SEARCH = SUMMARY + """
            WHERE c.removed_at IS NULL
              AND (c.slug LIKE :prefix ESCAPE '\\'
                   OR (' ' || lower(immutable_unaccent(c.name))) LIKE lower(immutable_unaccent(:wordPrefix)) ESCAPE '\\'
                   OR word_similarity(lower(immutable_unaccent(:term)), lower(immutable_unaccent(c.name))) > :threshold)
            ORDER BY CASE
                         WHEN c.slug = :term THEN 0
                         WHEN c.slug LIKE :prefix ESCAPE '\\' THEN 1
                         ELSE 2
                     END,
                     word_similarity(lower(immutable_unaccent(:term)), lower(immutable_unaccent(c.name))) DESC,
                     c.member_count DESC,
                     c.slug
            LIMIT :limit
            """;

    private static final String POPULAR = SUMMARY + """
            WHERE c.removed_at IS NULL
            ORDER BY c.member_count DESC, c.id
            LIMIT :limit
            """;

    private static final String SUGGESTED = SUMMARY + """
            WHERE c.removed_at IS NULL
              AND NOT EXISTS (SELECT 1 FROM community_members m WHERE m.community_id = c.id AND m.user_id = :viewer)
              AND EXISTS (SELECT 1 FROM community_topics t JOIN user_topics u ON u.topic_slug = t.topic_slug
                          WHERE t.community_id = c.id AND u.user_id = :viewer)
            ORDER BY (SELECT count(*) FROM community_topics t JOIN user_topics u ON u.topic_slug = t.topic_slug
                      WHERE t.community_id = c.id AND u.user_id = :viewer) DESC,
                     c.member_count DESC, c.id
            LIMIT :limit
            """;

    private static final String JOINED = SUMMARY + """
            JOIN community_members mine ON mine.community_id = c.id AND mine.user_id = :viewer
            WHERE c.removed_at IS NULL
            ORDER BY mine.joined_at DESC, c.id
            LIMIT :limit
            """;

    private static final double FUZZY_THRESHOLD = 0.4;

    private final JdbcClient jdbcClient;

    @Transactional(readOnly = true)
    public Optional<CommunityView> bySlug(UUID viewerId, String slug) {
        return jdbcClient.sql(DETAIL)
                .param("viewer", viewerId)
                .param("slug", slug)
                .query((row, index) -> toView(row))
                .optional();
    }

    @Transactional(readOnly = true)
    public List<CommunitySummary> search(UUID viewerId, String query, int limit) {
        String term = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        if (term.isEmpty()) {
            return summaries(POPULAR, viewerId, limit);
        }
        String escaped = term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return jdbcClient.sql(SEARCH)
                .param("viewer", viewerId)
                .param("term", term)
                .param("prefix", escaped + "%")
                .param("wordPrefix", "% " + escaped + "%")
                .param("threshold", FUZZY_THRESHOLD)
                .param("limit", limit)
                .query((row, index) -> toSummary(row))
                .list();
    }

    @Transactional(readOnly = true)
    public List<CommunitySummary> suggested(UUID viewerId, int limit) {
        return summaries(SUGGESTED, viewerId, limit);
    }

    @Transactional(readOnly = true)
    public List<CommunitySummary> joined(UUID viewerId, int limit) {
        return summaries(JOINED, viewerId, limit);
    }

    private List<CommunitySummary> summaries(String sql, UUID viewerId, int limit) {
        return jdbcClient.sql(sql)
                .param("viewer", viewerId)
                .param("limit", limit)
                .query((row, index) -> toSummary(row))
                .list();
    }

    private static CommunitySummary toSummary(ResultSet row) throws SQLException {
        return new CommunitySummary(
                row.getObject("id", UUID.class),
                row.getString("slug"),
                row.getString("name"),
                row.getString("description"),
                JoinPolicy.valueOf(row.getString("join_policy")),
                row.getInt("member_count"),
                row.getBoolean("joined"));
    }

    private static CommunityView toView(ResultSet row) throws SQLException {
        String role = row.getString("viewer_role");
        return new CommunityView(
                row.getObject("id", UUID.class),
                row.getString("slug"),
                row.getString("name"),
                row.getString("description"),
                JoinPolicy.valueOf(row.getString("join_policy")),
                topics(row.getArray("topics")),
                row.getInt("member_count"),
                row.getInt("slow_mode_seconds"),
                row.getTimestamp("created_at").toInstant(),
                new CommunityView.Viewer(role == null ? null : CommunityRole.valueOf(role), row.getBoolean("requested")));
    }

    private static List<String> topics(Array array) throws SQLException {
        return Arrays.asList((String[]) array.getArray());
    }

    public record CommunitySummary(UUID id, String slug, String name, String description, JoinPolicy joinPolicy,
                                   int memberCount, boolean joined) {
    }

    public record CommunityView(UUID id, String slug, String name, String description, JoinPolicy joinPolicy,
                                List<String> topics, int memberCount, int slowModeSeconds, Instant createdAt,
                                Viewer viewer) {

        public record Viewer(CommunityRole role, boolean requested) {
        }
    }
}
