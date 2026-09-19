package com.application.devhub;

import com.application.devhub.idempotency.IdempotencyRecordCleanupJob;
import com.application.devhub.otp.OneTimeCodeCleanupJob;
import com.application.devhub.otp.OneTimeCodeIssuer;
import com.application.devhub.otp.OneTimeCodePurpose;
import com.application.devhub.outbox.OutboxCleanupJob;
import com.application.devhub.outbox.OutboxEvent;
import com.application.devhub.outbox.OutboxEventRepository;
import com.application.devhub.outbox.OutboxEventType;
import com.application.devhub.session.RefreshTokenCleanupJob;
import com.application.devhub.session.RefreshTokenService;
import com.application.devhub.user.User;
import com.application.devhub.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CleanupJobsIntegrationTest extends IntegrationTest {

    @Autowired
    private OutboxCleanupJob outboxCleanup;

    @Autowired
    private RefreshTokenCleanupJob refreshTokenCleanup;

    @Autowired
    private OneTimeCodeCleanupJob oneTimeCodeCleanup;

    @Autowired
    private IdempotencyRecordCleanupJob idempotencyCleanup;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private OneTimeCodeIssuer codeIssuer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void outboxKeepsPendingAndRecentEventsAndFailedOnesLonger() {
        UUID oldSent = event("SENT", "8 days");
        UUID recentSent = event("SENT", "1 day");
        UUID oldFailed = event("FAILED", "31 days");
        UUID recentFailed = event("FAILED", "8 days");
        UUID pending = outboxRepository.save(new OutboxEvent(OutboxEventType.EMAIL_VERIFICATION, "{}")).getId();

        assertThat(outboxCleanup.purge()).isEqualTo(2);

        assertThat(outboxRepository.findAllById(List.of(oldSent, recentSent, oldFailed, recentFailed, pending)))
                .extracting(OutboxEvent::getId)
                .containsExactlyInAnyOrder(recentSent, recentFailed, pending);
    }

    @Test
    void refreshTokensAreKeptUntilTheyExpireEvenWhenRevoked() {
        User user = user("linus");
        refreshTokenService.startSession(user.getId());
        refreshTokenService.startSession(user.getId());
        jdbcTemplate.update("""
                UPDATE refresh_tokens SET expires_at = now() - interval '1 minute'
                WHERE id = (SELECT id FROM refresh_tokens ORDER BY created_at LIMIT 1)
                """);
        refreshTokenService.startSession(user.getId());

        assertThat(refreshTokenCleanup.purge()).isEqualTo(1);
        assertThat(count("refresh_tokens")).isEqualTo(2);
    }

    @Test
    void codesAreKeptWhileTheirDailyCapStillApplies() {
        codeIssuer.issue(user("stale"), OneTimeCodePurpose.PASSWORD_RESET);
        codeIssuer.issue(user("capped"), OneTimeCodePurpose.PASSWORD_RESET);
        jdbcTemplate.update("UPDATE one_time_codes SET expires_at = now() - interval '1 minute'");
        jdbcTemplate.update("""
                UPDATE one_time_codes SET window_started_at = now() - interval '2 days'
                WHERE user_id = (SELECT id FROM users WHERE username = 'stale')
                """);

        assertThat(oneTimeCodeCleanup.purge()).isEqualTo(1);
        assertThat(count("one_time_codes")).isEqualTo(1);
    }

    @Test
    void idempotencyRecordsAreRemovedOnceExpired() {
        jdbcTemplate.update("""
                INSERT INTO idempotency_records (id, idempotency_key, endpoint, request_hash, status, expires_at)
                VALUES (gen_random_uuid(), 'old', 'POST /auth/signup', 'h', 'COMPLETED', now() - interval '1 minute'),
                       (gen_random_uuid(), 'new', 'POST /auth/signup', 'h', 'COMPLETED', now() + interval '1 hour')
                """);

        assertThat(idempotencyCleanup.purge()).isEqualTo(1);
        assertThat(count("idempotency_records")).isEqualTo(1);
    }

    private UUID event(String status, String processedAgo) {
        UUID id = outboxRepository.save(new OutboxEvent(OutboxEventType.EMAIL_VERIFICATION, "{}")).getId();
        jdbcTemplate.update("UPDATE outbox_events SET status = ?, processed_at = now() - ?::interval WHERE id = ?",
                status, processedAgo, id);
        return id;
    }

    private User user(String username) {
        return userRepository.save(new User(username, username, username + "@dev.io", "{noop}x"));
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }
}
