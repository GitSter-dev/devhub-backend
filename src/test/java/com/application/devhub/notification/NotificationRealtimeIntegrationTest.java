package com.application.devhub.notification;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import com.application.devhub.outbox.OutboxRelay;
import com.application.devhub.realtime.StompTestSocket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "devhub.scheduling.enabled=false", "devhub.rate-limit.enabled=false", "devhub.push.enabled=false"})
class NotificationRealtimeIntegrationTest extends IntegrationTest {

    private static final long SUBSCRIBE_SETTLE_MS = 300;

    @LocalServerPort
    private int port;

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

    @Test
    void theRecipientHearsThatTheirNotificationsChanged() throws Exception {
        NotificationFixtures fixtures = new NotificationFixtures(mockMvc, jsonMapper, jdbcTemplate, outboxRelay);
        users.verified("ada");
        users.verified("ken");
        String ada = users.bearer("ada");
        UUID post = fixtures.publish(ada, "Live");
        StompTestSocket socket = StompTestSocket.connect(port, ada.substring("Bearer ".length()));
        assertThat(socket.nextFrame()).startsWith("CONNECTED");
        socket.subscribe("/user/queue/events");
        Thread.sleep(SUBSCRIBE_SETTLE_MS);

        fixtures.like(users.bearer("ken"), post);
        fixtures.drain();

        assertThat(socket.nextFrame()).startsWith("MESSAGE").contains("\"type\":\"NOTIFICATIONS_CHANGED\"");
    }
}
