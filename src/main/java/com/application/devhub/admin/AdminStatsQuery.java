package com.application.devhub.admin;

import com.application.devhub.admin.AdminStatsViews.Day;
import com.application.devhub.admin.AdminStatsViews.Overview;
import com.application.devhub.admin.AdminStatsViews.Totals;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminStatsQuery {

    private static final String TOTALS = """
            SELECT (SELECT count(*) FROM users WHERE deleted_at IS NULL) AS users,
                   (SELECT count(*) FROM users WHERE deleted_at IS NULL AND email_verified_at IS NOT NULL) AS verified,
                   (SELECT count(*) FROM posts WHERE deleted_at IS NULL AND removed_at IS NULL) AS posts,
                   (SELECT count(*) FROM moderation_cases WHERE status = 'OPEN') AS open_cases,
                   (SELECT count(*) FROM users WHERE banned_at IS NULL AND suspended_until > now()) AS suspended,
                   (SELECT count(*) FROM users WHERE banned_at IS NOT NULL AND deleted_at IS NULL) AS banned
            """;

    private static final String DAILY = """
            WITH days AS (
                SELECT CAST(generate_series(CAST(:from AS date), CAST(:to AS date), interval '1 day') AS date) AS day
            )
            SELECT d.day,
                   coalesce(s.n, 0) AS signups,
                   coalesce(p.n, 0) AS posts,
                   coalesce(r.n, 0) AS reports,
                   coalesce(a.n, 0) AS actions
            FROM days d
            LEFT JOIN (%s) s ON s.day = d.day
            LEFT JOIN (%s) p ON p.day = d.day
            LEFT JOIN (%s) r ON r.day = d.day
            LEFT JOIN (%s) a ON a.day = d.day
            ORDER BY d.day
            """.formatted(perDay("users"), perDay("posts"), perDay("reports"), perDay("moderation_actions"));

    private final JdbcClient jdbcClient;

    @Transactional(readOnly = true)
    public Overview overview(int days) {
        LocalDate to = LocalDate.now(ZoneOffset.UTC);
        LocalDate from = to.minusDays(days - 1L);
        Totals totals = jdbcClient.sql(TOTALS)
                .query((row, index) -> new Totals(row.getLong("users"), row.getLong("verified"), row.getLong("posts"),
                        row.getLong("open_cases"), row.getLong("suspended"), row.getLong("banned")))
                .single();
        List<Day> daily = jdbcClient.sql(DAILY)
                .param("from", from)
                .param("to", to)
                .param("since", Timestamp.from(from.atStartOfDay(ZoneOffset.UTC).toInstant()))
                .query((row, index) -> new Day(row.getObject("day", LocalDate.class), row.getLong("signups"),
                        row.getLong("posts"), row.getLong("reports"), row.getLong("actions")))
                .list();
        return new Overview(totals, daily);
    }

    private static String perDay(String table) {
        return "SELECT CAST(created_at AT TIME ZONE 'UTC' AS date) AS day, count(*) AS n FROM " + table
                + " WHERE created_at >= :since GROUP BY 1";
    }
}
