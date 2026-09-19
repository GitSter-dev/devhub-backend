package com.application.devhub.notification;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import com.application.devhub.outbox.OutboxRelay;
import com.application.devhub.push.PushDispatcher;
import com.application.devhub.push.PushMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class NotificationPushIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private TestUsers users;

    @Autowired
    private NotificationPusher pusher;

    @MockitoBean
    private PushDispatcher pushDispatcher;

    private NotificationFixtures fixtures;
    private UUID adaId;
    private String ada;
    private String ken;
    private String linus;
    private UUID post;

    @BeforeEach
    void setUp() throws Exception {
        fixtures = new NotificationFixtures(mockMvc, jsonMapper, jdbcTemplate, outboxRelay);
        adaId = users.verified("ada", "Ada");
        users.verified("ken", "Ken");
        users.verified("linus", "Linus");
        ada = users.bearer("ada");
        ken = users.bearer("ken");
        linus = users.bearer("linus");
        post = fixtures.publish(ada, "Shipped the parser");
    }

    @Test
    void theFirstActivityWaitsOnlyForTheBurstWindow() throws Exception {
        fixtures.like(ken, post);
        fixtures.drain();

        Map<String, Object> row = row();
        assertThat(row.get("pushed_count")).isEqualTo(0);
        assertThat((Boolean) jdbcTemplate.queryForObject(
                "SELECT push_due_at BETWEEN now() + interval '3 seconds' AND now() + interval '6 seconds' FROM notifications",
                Boolean.class)).isTrue();
    }

    @Test
    void aBurstGoesOutAsOnePushNamingEveryone() throws Exception {
        fixtures.like(ken, post);
        fixtures.like(linus, post);
        fixtures.drain();
        makeDue();

        assertThat(pusher.pushDue()).isEqualTo(1);

        ArgumentCaptor<PushMessage> push = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushDispatcher).dispatch(eq(adaId), push.capture());
        assertThat(push.getValue().title()).isEqualTo("Linus and Ken liked your post");
        assertThat(push.getValue().body()).isEqualTo("Shipped the parser");
        assertThat(push.getValue().group()).isEqualTo("activity:POST_LIKED:" + post);
        assertThat(push.getValue().data()).containsEntry("kind", "activity").containsEntry("type", "POST_LIKED")
                .containsEntry("actorUsername", "linus");
        assertThat(row().get("pushed_count")).isEqualTo(1);
        assertThat(row().get("push_due_at")).isNull();
    }

    @Test
    void afterTheIndividualPushesNewActivityWaitsForTheDigest() throws Exception {
        fixtures.like(ken, post);
        fixtures.drain();
        jdbcTemplate.update("UPDATE notifications SET pushed_count = 3, pushed_at = now(), push_due_at = NULL");

        fixtures.like(linus, post);
        fixtures.drain();

        assertThat((Boolean) jdbcTemplate.queryForObject(
                "SELECT push_due_at > now() + interval '14 minutes' FROM notifications", Boolean.class)).isTrue();
    }

    @Test
    void seenNotificationsAreNeverPushed() throws Exception {
        fixtures.like(ken, post);
        fixtures.drain();
        fixtures.markSeen(ada, "2999-01-01T00:00:00Z");
        makeDue();

        pusher.pushDue();

        verify(pushDispatcher, never()).dispatch(any(), any());
    }

    @Test
    void aWithdrawnGroupSkipsItsPush() throws Exception {
        fixtures.like(ken, post);
        fixtures.drain();
        fixtures.unlike(ken, post);
        fixtures.drain();
        makeDue();

        assertThat(pusher.pushDue()).isEqualTo(1);

        verify(pushDispatcher, never()).dispatch(any(), any());
        assertThat(row().get("push_due_at")).isNull();
    }

    private void makeDue() {
        jdbcTemplate.update("UPDATE notifications SET push_due_at = now() - interval '1 second' WHERE seen_at IS NULL");
    }

    private Map<String, Object> row() {
        return jdbcTemplate.queryForMap("SELECT pushed_count, push_due_at FROM notifications");
    }
}
