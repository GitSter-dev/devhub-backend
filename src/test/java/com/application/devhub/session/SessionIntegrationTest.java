package com.application.devhub.session;

import com.application.devhub.IntegrationTest;
import com.application.devhub.idempotency.Idempotent;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SessionIntegrationTest extends IntegrationTest {

    private static final String PASSWORD = "supersecret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    private User linus;

    @BeforeEach
    void createUsers() {
        linus = new User("Linus_T", "Linus Torvalds", "linus@dev.io", passwordEncoder.encode(PASSWORD));
        linus.markEmailVerified();
        linus = userRepository.save(linus);
        userRepository.save(new User("grace", "Grace Hopper", "grace@dev.io", passwordEncoder.encode(PASSWORD)));
    }

    @Test
    void loginWithEmailIssuesWorkingTokenPair() throws Exception {
        JsonNode tokens = data(login("linus@dev.io", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessTokenExpiresAt").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.refreshTokenExpiresAt").exists()));

        mockMvc.perform(get("/test/whoami").header(HttpHeaders.AUTHORIZATION, bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value(linus.getId().toString()));
    }

    @Test
    void loginWithUsernameIgnoresCase() throws Exception {
        login("linus_t", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void wrongPasswordAndUnknownUserAreIndistinguishable() throws Exception {
        JsonNode wrongPassword = error(login("linus@dev.io", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS")));
        JsonNode unknownUser = error(login("nobody@dev.io", PASSWORD).andExpect(status().isUnauthorized()));

        assertThat(unknownUser).isEqualTo(wrongPassword);
    }

    @Test
    void unverifiedStatusIsOnlyRevealedWithTheRightPassword() throws Exception {
        login("grace", PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("EMAIL_NOT_VERIFIED"));

        login("grace", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void refreshRotatesTheRefreshToken() throws Exception {
        JsonNode first = data(login("linus@dev.io", PASSWORD));

        JsonNode second = data(refresh(refreshToken(first)).andExpect(status().isOk()));

        assertThat(refreshToken(second)).isNotEqualTo(refreshToken(first));
        mockMvc.perform(get("/test/whoami").header(HttpHeaders.AUTHORIZATION, bearer(second)))
                .andExpect(status().isOk());
    }

    @Test
    void reusingARotatedTokenRevokesTheWholeSession() throws Exception {
        String original = refreshToken(data(login("linus@dev.io", PASSWORD)));
        String rotated = refreshToken(data(refresh(original)));

        refresh(original)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));

        refresh(rotated).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheSessionAndIsIdempotent() throws Exception {
        String token = refreshToken(data(login("linus@dev.io", PASSWORD)));

        logout(token).andExpect(status().isOk());
        refresh(token).andExpect(status().isUnauthorized());

        logout("not-a-real-token").andExpect(status().isOk());
    }

    @Test
    void loginOnAnotherDeviceReplacesTheFirstSession() throws Exception {
        JsonNode firstDevice = data(login("linus@dev.io", PASSWORD));
        JsonNode secondDevice = data(login("linus_t", PASSWORD));

        refresh(refreshToken(firstDevice))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("SESSION_REPLACED"));
        refresh(refreshToken(secondDevice)).andExpect(status().isOk());
    }

    @Test
    void replacedDeviceKeepsItsAccessTokenUntilItExpires() throws Exception {
        JsonNode firstDevice = data(login("linus@dev.io", PASSWORD));
        login("linus@dev.io", PASSWORD).andExpect(status().isOk());

        mockMvc.perform(get("/test/whoami").header(HttpHeaders.AUTHORIZATION, bearer(firstDevice)))
                .andExpect(status().isOk());
    }

    @Test
    void loggedOutSessionIsNotReportedAsReplaced() throws Exception {
        String token = refreshToken(data(login("linus@dev.io", PASSWORD)));
        logout(token);

        refresh(token)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void lostRefreshResponseCanBeReplayedWithTheSameKey() throws Exception {
        String original = refreshToken(data(login("linus@dev.io", PASSWORD)));
        String lost = refreshToken(data(refresh(original, "refresh-key-0001")));

        JsonNode replayed = data(refresh(original, "refresh-key-0001").andExpect(status().isOk()));

        refresh(lost).andExpect(status().isUnauthorized());
        refresh(refreshToken(replayed)).andExpect(status().isOk());
    }

    @Test
    void reuseWithADifferentKeyIsStillTreatedAsTheft() throws Exception {
        String original = refreshToken(data(login("linus@dev.io", PASSWORD)));
        String rotated = refreshToken(data(refresh(original, "refresh-key-0001")));

        refresh(original, "another-key-0002")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
        refresh(rotated).andExpect(status().isUnauthorized());
    }

    @Test
    void replayIsRefusedOnceTheSuccessorWasUsed() throws Exception {
        String original = refreshToken(data(login("linus@dev.io", PASSWORD)));
        String rotated = refreshToken(data(refresh(original, "refresh-key-0001")));
        refresh(rotated).andExpect(status().isOk());

        refresh(original, "refresh-key-0001").andExpect(status().isUnauthorized());
    }

    @Test
    void replayIsRefusedOutsideTheWindow() throws Exception {
        String original = refreshToken(data(login("linus@dev.io", PASSWORD)));
        refresh(original, "refresh-key-0001");
        jdbcTemplate.update("UPDATE refresh_tokens SET used_at = now() - interval '10 minutes' WHERE used_at IS NOT NULL");

        refresh(original, "refresh-key-0001").andExpect(status().isUnauthorized());
    }

    @Test
    void expiredRefreshTokenIsRejected() throws Exception {
        String token = refreshToken(data(login("linus@dev.io", PASSWORD)));
        jdbcTemplate.update("UPDATE refresh_tokens SET expires_at = now() - interval '1 minute'");

        refresh(token).andExpect(status().isUnauthorized());
    }

    @Test
    void onlyTheHashOfTheRefreshTokenIsStored() throws Exception {
        String token = refreshToken(data(login("linus@dev.io", PASSWORD)));

        List<String> storedHashes = jdbcTemplate.queryForList("SELECT token_hash FROM refresh_tokens", String.class);

        assertThat(storedHashes).singleElement()
                .isNotEqualTo(token)
                .satisfies(hash -> assertThat(hash).hasSize(64));
    }

    private ResultActions login(String identifier, String password) throws Exception {
        return postJson("/auth/login", "{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(identifier, password));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return postJson("/auth/refresh", "{\"refreshToken\":\"%s\"}".formatted(refreshToken));
    }

    private ResultActions refresh(String refreshToken, String idempotencyKey) throws Exception {
        return mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .header(Idempotent.HEADER, idempotencyKey)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)));
    }

    private ResultActions logout(String refreshToken) throws Exception {
        return postJson("/auth/logout", "{\"refreshToken\":\"%s\"}".formatted(refreshToken));
    }

    private ResultActions postJson(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode data(ResultActions result) throws Exception {
        return body(result).get("data");
    }

    private JsonNode error(ResultActions result) throws Exception {
        return body(result).get("error");
    }

    private JsonNode body(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private static String refreshToken(JsonNode tokens) {
        return tokens.get("refreshToken").asString();
    }

    private static String bearer(JsonNode tokens) {
        return "Bearer " + tokens.get("accessToken").asString();
    }
}
