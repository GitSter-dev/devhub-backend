package com.application.devhub.admin;

import com.application.devhub.admin.AdminUserViews.UserDetail;
import com.application.devhub.admin.AdminUserViews.UserSummary;
import com.application.devhub.common.api.ApiException;
import com.application.devhub.moderation.ModerationQueries;
import com.application.devhub.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminUserQueries {

    private static final int MIN_QUERY_LENGTH = 2;
    private static final int MAX_QUERY_LENGTH = 100;
    private static final int SEARCH_LIMIT = 20;
    private static final int CASE_LIMIT = 50;
    private static final double FUZZY_THRESHOLD = 0.4;

    private static final String COLUMNS = """
            SELECT u.id, u.username, u.display_name, u.email, u.role, u.created_at, u.email_verified_at,
                   u.suspended_until, u.banned_at, u.deactivated_at, u.deleted_at
            FROM users u
            """;

    private static final String SEARCH = COLUMNS + """
            WHERE lower(u.email) = :term
               OR lower(u.username) LIKE :usernamePrefix ESCAPE '\\'
               OR (' ' || lower(immutable_unaccent(u.display_name))) LIKE lower(immutable_unaccent(:wordPrefix)) ESCAPE '\\'
               OR word_similarity(:term, lower(u.username)) > :threshold
               OR word_similarity(lower(immutable_unaccent(:term)), lower(immutable_unaccent(u.display_name))) > :threshold
            ORDER BY CASE
                         WHEN lower(u.email) = :term OR lower(u.username) = :term THEN 0
                         WHEN lower(u.username) LIKE :usernamePrefix ESCAPE '\\' THEN 1
                         WHEN (' ' || lower(immutable_unaccent(u.display_name))) LIKE lower(immutable_unaccent(:wordPrefix)) ESCAPE '\\' THEN 2
                         ELSE 3
                     END,
                     greatest(word_similarity(:term, lower(u.username)),
                              word_similarity(lower(immutable_unaccent(:term)), lower(immutable_unaccent(u.display_name)))) DESC,
                     u.username
            LIMIT :limit
            """;

    private static final String COUNTS = """
            SELECT (SELECT count(*) FROM posts p WHERE p.author_id = :userId AND p.deleted_at IS NULL) AS posts,
                   (SELECT count(*) FROM reports r WHERE r.reporter_id = :userId) AS filed,
                   (SELECT count(*) FROM reports r WHERE r.target_owner_id = :userId) AS against
            """;

    private final JdbcClient jdbcClient;
    private final ModerationQueries moderationQueries;

    @Transactional(readOnly = true)
    public List<UserSummary> search(String query) {
        String term = normalize(query);
        if (term.length() < MIN_QUERY_LENGTH) {
            return List.of();
        }
        String escaped = escapeLike(term);
        Instant now = Instant.now();
        return jdbcClient.sql(SEARCH)
                .param("term", term)
                .param("usernamePrefix", escaped + "%")
                .param("wordPrefix", "% " + escaped + "%")
                .param("threshold", FUZZY_THRESHOLD)
                .param("limit", SEARCH_LIMIT)
                .query((row, index) -> toSummary(row, now))
                .list();
    }

    @Transactional(readOnly = true)
    public UserDetail detail(UUID userId) {
        Instant now = Instant.now();
        Account account = jdbcClient.sql(COLUMNS + " WHERE u.id = :userId")
                .param("userId", userId)
                .query((row, index) -> new Account(toSummary(row, now), instant(row, "email_verified_at"),
                        instant(row, "suspended_until"), instant(row, "banned_at"), instant(row, "deactivated_at"),
                        instant(row, "deleted_at")))
                .optional()
                .orElseThrow(ApiException::notFound);
        Counts counts = jdbcClient.sql(COUNTS)
                .param("userId", userId)
                .query((row, index) -> new Counts(row.getLong("posts"), row.getLong("filed"), row.getLong("against")))
                .single();
        return new UserDetail(account.summary(), account.emailVerifiedAt(), account.suspendedUntil(),
                account.bannedAt(), account.deactivatedAt(), account.deletedAt(), counts.posts(), counts.filed(),
                counts.against(), moderationQueries.casesOwnedBy(userId, CASE_LIMIT));
    }

    private static UserSummary toSummary(ResultSet row, Instant now) throws SQLException {
        AccountStatus status = AccountStatus.of(instant(row, "email_verified_at"), instant(row, "suspended_until"),
                instant(row, "banned_at"), instant(row, "deactivated_at"), instant(row, "deleted_at"), now);
        return new UserSummary(row.getObject("id", UUID.class), row.getString("username"),
                row.getString("display_name"), row.getString("email"), Role.valueOf(row.getString("role")), status,
                row.getTimestamp("created_at").toInstant());
    }

    private static Instant instant(ResultSet row, String column) throws SQLException {
        Timestamp value = row.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static String normalize(String query) {
        String term = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        term = term.startsWith("@") ? term.substring(1).strip() : term;
        return term.length() > MAX_QUERY_LENGTH ? term.substring(0, MAX_QUERY_LENGTH) : term;
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private record Account(UserSummary summary, Instant emailVerifiedAt, Instant suspendedUntil, Instant bannedAt,
                           Instant deactivatedAt, Instant deletedAt) {
    }

    private record Counts(long posts, long filed, long against) {
    }
}
