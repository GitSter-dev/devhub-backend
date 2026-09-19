package com.application.devhub.chat;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import com.application.devhub.realtime.StompTestSocket;
import org.junit.jupiter.api.BeforeEach;
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
class ChatRealtimeIntegrationTest extends IntegrationTest {

    private static final String EVENTS = "/user/queue/events";
    private static final long SUBSCRIBE_SETTLE_MS = 300;
    private static final long SILENCE_MS = 700;

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    private ChatFixtures chat;
    private String adaBearer;
    private String graceBearer;
    private String linusBearer;
    private UUID conversation;

    @BeforeEach
    void signIn() throws Exception {
        chat = new ChatFixtures(mockMvc, jsonMapper);
        UUID ada = users.verified("ada");
        UUID grace = users.verified("grace");
        users.verified("linus");
        jdbcTemplate.update("INSERT INTO follows (follower_id, followee_id) VALUES (?, ?)", grace, ada);
        adaBearer = users.bearer("ada");
        graceBearer = users.bearer("grace");
        linusBearer = users.bearer("linus");
        conversation = chat.direct(adaBearer, grace);
    }

    @Test
    void membersReceiveNewMessagesLiveAndOutsidersDoNot() throws Exception {
        StompTestSocket grace = listen(graceBearer);
        StompTestSocket linus = listen(linusBearer);

        chat.send(adaBearer, conversation, "live hello");

        String frame = grace.nextFrame();
        assertThat(frame).startsWith("MESSAGE").contains("\"type\":\"MESSAGE_CREATED\"").contains("live hello");
        assertThat(linus.nextFrameWithin(SILENCE_MS)).isNull();
    }

    @Test
    void theSenderLearnsWhenTheirMessageIsRead() throws Exception {
        chat.send(adaBearer, conversation, "read me");
        StompTestSocket ada = listen(adaBearer);

        chat.receipts(graceBearer, conversation, 1, 1);

        assertThat(ada.nextFrame()).contains("\"type\":\"RECEIPT_UPDATED\"").contains("\"readSeq\":1");
    }

    @Test
    void typingIsRelayedToTheOtherMember() throws Exception {
        StompTestSocket ada = listen(adaBearer);
        StompTestSocket grace = listen(graceBearer);

        ada.send("SEND\ndestination:/app/conversations/" + conversation + "/typing\n\n\0");

        assertThat(grace.nextFrame()).contains("\"type\":\"TYPING\"").contains("\"displayName\":\"ada\"");
        assertThat(ada.nextFrameWithin(SILENCE_MS)).isNull();
    }

    @Test
    void outsidersCannotMakeAnyoneSeeThemTyping() throws Exception {
        StompTestSocket grace = listen(graceBearer);
        StompTestSocket linus = listen(linusBearer);

        linus.send("SEND\ndestination:/app/conversations/" + conversation + "/typing\n\n\0");

        assertThat(grace.nextFrameWithin(SILENCE_MS)).isNull();
    }

    private StompTestSocket listen(String bearer) throws Exception {
        StompTestSocket socket = StompTestSocket.connect(port, bearer.substring("Bearer ".length()));
        assertThat(socket.nextFrame()).startsWith("CONNECTED");
        socket.subscribe(EVENTS);
        Thread.sleep(SUBSCRIBE_SETTLE_MS);
        return socket;
    }
}
