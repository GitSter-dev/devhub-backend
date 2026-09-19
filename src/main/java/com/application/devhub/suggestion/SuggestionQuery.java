package com.application.devhub.suggestion;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SuggestionQuery {

    private static final String SQL = """
            SELECT u.id, u.username, u.display_name,
                   array_remove(array_agg(ut.topic_slug ORDER BY ut.topic_slug), NULL) AS shared_topics,
                   (SELECT count(*) FROM follows f WHERE f.followee_id = u.id) AS followers
            FROM users u
            LEFT JOIN user_topics ut
                   ON ut.user_id = u.id
                  AND ut.topic_slug IN (SELECT mine.topic_slug FROM user_topics mine WHERE mine.user_id = :me)
            WHERE u.id <> :me
              AND u.email_verified_at IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM follows f WHERE f.follower_id = :me AND f.followee_id = u.id)
            GROUP BY u.id
            ORDER BY count(ut.topic_slug) DESC, followers DESC, u.created_at DESC
            LIMIT :limit
            """;

    private final JdbcClient jdbcClient;

    @Transactional(readOnly = true)
    public List<SuggestionResponse> forUser(UUID userId, int limit) {
        return jdbcClient.sql(SQL)
                .param("me", userId)
                .param("limit", limit)
                .query((row, index) -> toSuggestion(row))
                .list();
    }

    private static SuggestionResponse toSuggestion(ResultSet row) throws SQLException {
        return SuggestionResponse.of(row.getObject("id", UUID.class), row.getString("username"),
                row.getString("display_name"), topicsOf(row.getArray("shared_topics")));
    }

    private static List<String> topicsOf(Array array) throws SQLException {
        return array == null ? List.of() : List.of((String[]) array.getArray());
    }
}
