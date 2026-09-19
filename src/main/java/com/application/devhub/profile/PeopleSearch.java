package com.application.devhub.profile;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PeopleSearch {

    private static final String SQL = """
            SELECT u.id, u.username, u.display_name,
                   EXISTS (SELECT 1 FROM follows v WHERE v.follower_id = :me AND v.followee_id = u.id) AS following,
                   (SELECT count(*) FROM follows f WHERE f.followee_id = u.id) AS followers
            FROM users u
            WHERE u.id <> :me
              AND u.email_verified_at IS NOT NULL
              AND (lower(u.username) LIKE :usernamePrefix ESCAPE '\\'
                   OR (' ' || lower(immutable_unaccent(u.display_name))) LIKE lower(immutable_unaccent(:wordPrefix)) ESCAPE '\\'
                   OR word_similarity(:term, lower(u.username)) > :threshold
                   OR word_similarity(lower(immutable_unaccent(:term)), lower(immutable_unaccent(u.display_name))) > :threshold)
            ORDER BY CASE
                         WHEN lower(u.username) = :term THEN 0
                         WHEN lower(u.username) LIKE :usernamePrefix ESCAPE '\\' THEN 1
                         WHEN (' ' || lower(immutable_unaccent(u.display_name))) LIKE lower(immutable_unaccent(:wordPrefix)) ESCAPE '\\' THEN 2
                         ELSE 3
                     END,
                     greatest(word_similarity(:term, lower(u.username)),
                              word_similarity(lower(immutable_unaccent(:term)), lower(immutable_unaccent(u.display_name)))) DESC,
                     following DESC,
                     followers DESC,
                     u.username
            LIMIT :limit
            """;

    private static final double FUZZY_THRESHOLD = 0.4;

    private final JdbcClient jdbcClient;

    @Transactional(readOnly = true)
    public List<PersonSummary> search(UUID viewerId, String query, int limit) {
        String term = normalize(query);
        if (term.isEmpty()) {
            return List.of();
        }
        String escaped = escapeLike(term);
        return jdbcClient.sql(SQL)
                .param("me", viewerId)
                .param("term", term)
                .param("usernamePrefix", escaped + "%")
                .param("wordPrefix", "% " + escaped + "%")
                .param("threshold", FUZZY_THRESHOLD)
                .param("limit", limit)
                .query((row, index) -> new PersonSummary(row.getObject("id", UUID.class), row.getString("username"),
                        row.getString("display_name"), row.getBoolean("following")))
                .list();
    }

    private static String normalize(String query) {
        String term = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        return term.startsWith("@") ? term.substring(1).strip() : term;
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
