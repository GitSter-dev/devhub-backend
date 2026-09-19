package com.application.devhub.device;

import com.application.devhub.IntegrationTest;
import com.application.devhub.outbox.OutboxEvent;
import com.application.devhub.outbox.OutboxEventRepository;
import com.application.devhub.outbox.OutboxEventType;
import com.application.devhub.session.RefreshTokenService;
import com.application.devhub.session.RevocationReason;
import com.application.devhub.user.User;
import com.application.devhub.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeviceRegistrationIntegrationTest extends IntegrationTest {

    private static final String PASSWORD = "supersecret";
    private static final String INSTALL = "install-aaaa-0001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    @BeforeEach
    void createUsers() {
        verifiedUser("linus");
        verifiedUser("grace");
    }

    @Test
    void registersTheInstallAndBindsItToTheSession() throws Exception {
        Session session = login("linus");

        register(session, INSTALL, "token-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.installationId").value(INSTALL))
                .andExpect(jsonPath("$.data.platform").value("ANDROID"))
                .andExpect(jsonPath("$.data.pushToken").doesNotExist());

        assertThat(row(INSTALL))
                .containsEntry("push_token", "token-1")
                .containsEntry("session_family_id", session.sessionId())
                .containsEntry("revoked_at", null);
    }

    @Test
    void reRegisteringTheSameInstallUpdatesInPlace() throws Exception {
        Session session = login("linus");
        register(session, INSTALL, "token-1").andExpect(status().isOk());

        register(session, INSTALL, "token-rotated").andExpect(status().isOk());

        assertThat(count("devices")).isEqualTo(1);
        assertThat(row(INSTALL)).containsEntry("push_token", "token-rotated");
    }

    @Test
    void anInstallUsedByAnotherAccountMovesToThatAccount() throws Exception {
        register(login("linus"), INSTALL, "token-1").andExpect(status().isOk());
        Session grace = login("grace");

        register(grace, INSTALL, "token-1").andExpect(status().isOk());

        assertThat(count("devices")).isEqualTo(1);
        assertThat(row(INSTALL)).containsEntry("user_id", userId("grace"));
    }

    @Test
    void aPushTokenSeenOnANewInstallIsReleasedFromTheOldOne() throws Exception {
        Session session = login("linus");
        register(session, INSTALL, "token-1").andExpect(status().isOk());

        register(session, "install-bbbb-0002", "token-1").andExpect(status().isOk());

        assertThat(row(INSTALL)).containsEntry("push_token", null).containsEntry("revocation_reason", "TOKEN_MOVED");
        assertThat(row("install-bbbb-0002")).containsEntry("push_token", "token-1");
    }

    @Test
    void invalidRegistrationsAreRejected() throws Exception {
        Session session = login("linus");

        mockMvc.perform(put("/devices/current").header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"installationId\":\"bad id!\",\"platform\":\"TOASTER\",\"pushToken\":\"\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/devices/current").contentType(MediaType.APPLICATION_JSON).content(body(INSTALL, "token-1")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anEndedSessionCannotRegisterEvenWithAValidAccessToken() throws Exception {
        Session session = login("linus");
        logout(session);

        register(session, INSTALL, "token-1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("SESSION_ENDED"));
    }

    @Test
    void loggingInElsewhereStopsPushesToTheOldDevice() throws Exception {
        register(login("linus"), INSTALL, "token-1").andExpect(status().isOk());

        login("linus");

        assertThat(row(INSTALL)).containsEntry("revocation_reason", "SESSION_ENDED");
    }

    @Test
    void loggingOutStopsPushes() throws Exception {
        Session session = login("linus");
        register(session, INSTALL, "token-1").andExpect(status().isOk());

        logout(session);

        assertThat(row(INSTALL)).containsEntry("revocation_reason", "SESSION_ENDED");
    }

    @Test
    void aPasswordResetStopsPushesEverywhere() throws Exception {
        register(login("linus"), INSTALL, "token-1").andExpect(status().isOk());

        refreshTokenService.revokeAllFor(UUID.fromString(userId("linus")), RevocationReason.PASSWORD_RESET);

        assertThat(row(INSTALL)).containsEntry("revocation_reason", "SESSION_ENDED");
    }

    @Test
    void refreshingTheSessionKeepsTheDeviceActive() throws Exception {
        Session session = login("linus");
        register(session, INSTALL, "token-1").andExpect(status().isOk());

        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(session.refreshToken())))
                .andExpect(status().isOk());

        assertThat(row(INSTALL)).containsEntry("revoked_at", null);
    }

    @Test
    void testNotificationIsQueuedForTheUser() throws Exception {
        Session session = login("linus");

        mockMvc.perform(post("/devices/current/test-notification").header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isAccepted());

        assertThat(outboxRepository.findAll()).extracting(OutboxEvent::getType).containsExactly(OutboxEventType.PUSH_TEST);
    }

    private ResultActions register(Session session, String installationId, String pushToken) throws Exception {
        return mockMvc.perform(put("/devices/current").header(HttpHeaders.AUTHORIZATION, session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(installationId, pushToken)));
    }

    private static String body(String installationId, String pushToken) {
        return """
                {"installationId":"%s","platform":"ANDROID","pushToken":"%s","deviceName":"Pixel 8","appVersion":"1.0.0"}
                """.formatted(installationId, pushToken);
    }

    private Session login(String username) throws Exception {
        String response = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = jsonMapper.readTree(response).get("data");
        String accessToken = data.get("accessToken").asString();
        return new Session(accessToken, data.get("refreshToken").asString(), sessionIdOf(accessToken));
    }

    private void logout(Session session) throws Exception {
        mockMvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(session.refreshToken())))
                .andExpect(status().isOk());
    }

    private String sessionIdOf(String accessToken) {
        String payload = new String(Base64.getUrlDecoder().decode(accessToken.split("\\.")[1]), StandardCharsets.UTF_8);
        return jsonMapper.readTree(payload).get("sid").asString();
    }

    private void verifiedUser(String username) {
        User user = new User(username, username, username + "@dev.io", passwordEncoder.encode(PASSWORD));
        user.markEmailVerified();
        userRepository.save(user);
    }

    private String userId(String username) {
        return jdbcTemplate.queryForObject("SELECT id::text FROM users WHERE username = ?", String.class, username);
    }

    private Map<String, Object> row(String installationId) {
        return jdbcTemplate.queryForMap("""
                SELECT user_id::text AS user_id, session_family_id::text AS session_family_id, push_token,
                       revoked_at, revocation_reason
                FROM devices WHERE installation_id = ?
                """, installationId);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private record Session(String accessToken, String refreshToken, String sessionId) {

        String bearer() {
            return "Bearer " + accessToken;
        }
    }
}
