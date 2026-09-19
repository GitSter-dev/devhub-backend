package com.application.devhub.realtime;

import com.application.devhub.IntegrationTest;
import com.application.devhub.user.User;
import com.application.devhub.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "devhub.scheduling.enabled=false", "devhub.rate-limit.enabled=false", "devhub.push.enabled=false"})
class RealtimeIntegrationTest extends IntegrationTest {

    private static final String PASSWORD = "supersecret";
    private static final long TIMEOUT_SECONDS = 5;

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JsonMapper jsonMapper;

    @BeforeEach
    void createUser() {
        User user = new User("ada", "Ada", "ada@dev.io", passwordEncoder.encode(PASSWORD));
        user.markEmailVerified();
        userRepository.save(user);
    }

    @Test
    void aClientWithAValidAccessTokenIsConnected() throws Exception {
        Socket socket = connect(login().accessToken());

        assertThat(socket.nextFrame()).startsWith("CONNECTED");
    }

    @Test
    void aClientWithoutATokenIsRejectedAndDisconnected() throws Exception {
        Socket socket = connect(null);

        assertThat(socket.nextFrame()).startsWith("ERROR").contains("message:UNAUTHORIZED");
        assertThat(socket.closeStatus()).isNotNull();
    }

    @Test
    void aForgedTokenIsRejected() throws Exception {
        Socket socket = connect("not-a-jwt");

        assertThat(socket.nextFrame()).startsWith("ERROR").contains("message:UNAUTHORIZED");
    }

    @Test
    void theAccessTokenOfAnEndedSessionIsRejected() throws Exception {
        Tokens tokens = login();
        logout(tokens);

        Socket socket = connect(tokens.accessToken());

        assertThat(socket.nextFrame()).startsWith("ERROR").contains("message:SESSION_ENDED");
    }

    @Test
    void aLoginElsewhereClosesTheOldSocketAsReplaced() throws Exception {
        Socket socket = connect(login().accessToken());
        assertThat(socket.nextFrame()).startsWith("CONNECTED");

        login();

        assertThat(socket.closeStatus()).isEqualTo(RealtimeCloseStatus.SESSION_REPLACED);
    }

    @Test
    void loggingOutClosesTheSocketAsEnded() throws Exception {
        Tokens tokens = login();
        Socket socket = connect(tokens.accessToken());
        assertThat(socket.nextFrame()).startsWith("CONNECTED");

        logout(tokens);

        assertThat(socket.closeStatus()).isEqualTo(RealtimeCloseStatus.SESSION_ENDED);
    }

    @Test
    void aClientHeartbeatKeepsTheConnectionUsable() throws Exception {
        Socket socket = connect(login().accessToken());
        assertThat(socket.nextFrame()).startsWith("CONNECTED");

        socket.send("\n");
        socket.send("SUBSCRIBE\nid:1\ndestination:/queue/everyone\n\n\0");

        assertThat(socket.nextFrame()).startsWith("ERROR").contains("message:FORBIDDEN");
    }

    @Test
    void subscribingOutsideTheUserQueuesIsForbidden() throws Exception {
        Socket socket = connect(login().accessToken());
        assertThat(socket.nextFrame()).startsWith("CONNECTED");

        socket.send("SUBSCRIBE\nid:1\ndestination:/queue/everyone\n\n\0");

        assertThat(socket.nextFrame()).startsWith("ERROR").contains("message:FORBIDDEN");
    }

    @Test
    void sendingToTheServerIsForbidden() throws Exception {
        Socket socket = connect(login().accessToken());
        assertThat(socket.nextFrame()).startsWith("CONNECTED");

        socket.send("SEND\ndestination:/queue/anything\n\nhello\0");

        assertThat(socket.nextFrame()).startsWith("ERROR").contains("message:FORBIDDEN");
    }

    private Socket connect(String accessToken) throws Exception {
        Socket socket = new Socket();
        new StandardWebSocketClient().execute(socket, "ws://localhost:" + port + RealtimeConfig.ENDPOINT)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        String authorization = accessToken == null ? "" : "Authorization:Bearer " + accessToken + "\n";
        socket.send("CONNECT\naccept-version:1.2\nheart-beat:0,0\n" + authorization + "\n\0");
        return socket;
    }

    private Tokens login() throws Exception {
        String response = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"ada\",\"password\":\"%s\"}".formatted(PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = jsonMapper.readTree(response).get("data");
        return new Tokens(data.get("accessToken").asString(), data.get("refreshToken").asString());
    }

    private void logout(Tokens tokens) throws Exception {
        mockMvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(tokens.refreshToken())))
                .andExpect(status().isOk());
    }

    private record Tokens(String accessToken, String refreshToken) {
    }

    private static final class Socket extends TextWebSocketHandler {

        private final BlockingQueue<String> frames = new LinkedBlockingQueue<>();
        private final CompletableFuture<WebSocketSession> session = new CompletableFuture<>();
        private final CompletableFuture<CloseStatus> closed = new CompletableFuture<>();

        @Override
        public void afterConnectionEstablished(WebSocketSession established) {
            session.complete(established);
        }

        @Override
        protected void handleTextMessage(WebSocketSession ignored, TextMessage message) {
            frames.add(message.getPayload());
        }

        @Override
        public void afterConnectionClosed(WebSocketSession ignored, CloseStatus status) {
            closed.complete(status);
        }

        void send(String frame) throws Exception {
            session.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).sendMessage(new TextMessage(frame));
        }

        String nextFrame() throws InterruptedException {
            return frames.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        CloseStatus closeStatus() throws Exception {
            return closed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }
}
