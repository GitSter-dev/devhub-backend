package com.application.devhub.account;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountDeletionIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestUsers users;

    @Autowired
    private AccountPurger purger;

    private UUID adaId;
    private String ada;
    private String ken;

    @BeforeEach
    void setUp() throws Exception {
        adaId = users.verified("ada", "Ada");
        users.verified("ken", "Ken");
        ada = users.bearer("ada");
        ken = users.bearer("ken");
    }

    @Test
    void deletionNeedsTheRightPasswordAndThenSignsYouOutEverywhere() throws Exception {
        requestDeletion(ada, "wrong-password").andExpect(status().isUnauthorized());

        requestDeletion(ada, TestUsers.PASSWORD).andExpect(status().isOk());

        assertThat(count("SELECT count(*) FROM refresh_tokens WHERE user_id = ? AND revoked_at IS NULL", adaId)).isZero();
        login("ada").andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("ACCOUNT_DEACTIVATED"));
        perform(ken, get("/users/ada/profile")).andExpect(status().isNotFound());
    }

    @Test
    void restoringInsideTheGracePeriodBringsTheAccountBack() throws Exception {
        requestDeletion(ada, TestUsers.PASSWORD).andExpect(status().isOk());

        mockMvc.perform(post("/auth/restore").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"ada\",\"password\":\"" + TestUsers.PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        login("ada").andExpect(status().isOk());
        perform(ken, get("/users/ada/profile")).andExpect(status().isOk());
    }

    @Test
    void anAccountPastTheGracePeriodIsAnonymizedAndItsTracesAreRemoved() throws Exception {
        UUID post = publish(ada, "Ada was here");
        perform(ken, put("/posts/{id}/like", post)).andExpect(status().isOk());
        perform(ada, put("/users/me/following/{id}", idOf("ken"))).andExpect(status().isOk());
        perform(ada, put("/users/me/blocks/{id}", users.verified("mallory"))).andExpect(status().isOk());
        requestDeletion(ada, TestUsers.PASSWORD).andExpect(status().isOk());
        jdbcTemplate.update("UPDATE users SET deactivated_at = now() - interval '31 days' WHERE id = ?", adaId);

        assertThat(purger.purgeDue()).isEqualTo(1);

        Map<String, Object> user = jdbcTemplate.queryForMap("SELECT username, display_name, email, bio, deleted_at FROM users WHERE id = ?", adaId);
        assertThat((String) user.get("username")).startsWith("deleted_");
        assertThat(user.get("display_name")).isEqualTo("Deleted user");
        assertThat((String) user.get("email")).endsWith("@deleted.devhub.invalid");
        assertThat(user.get("deleted_at")).isNotNull();
        assertThat(count("SELECT count(*) FROM follows WHERE follower_id = ? OR followee_id = ?", adaId, adaId)).isZero();
        assertThat(count("SELECT count(*) FROM blocks WHERE blocker_id = ?", adaId)).isZero();
        assertThat(count("SELECT count(*) FROM posts WHERE author_id = ? AND (body IS NOT NULL OR deleted_at IS NULL)", adaId)).isZero();
        assertThat(count("SELECT count(*) FROM held_usernames WHERE username_lower = 'ada'")).isEqualTo(1);
        login("ada").andExpect(status().isUnauthorized());
    }

    @Test
    void groupsAreLeftWithOwnershipHandedOver() throws Exception {
        UUID kenId = idOf("ken");
        UUID group = UUID.fromString(data(perform(ada, post("/conversations/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"Crew\", \"memberIds\": [\"" + kenId + "\"]}"))).get("id").asString());
        requestDeletion(ada, TestUsers.PASSWORD).andExpect(status().isOk());
        jdbcTemplate.update("UPDATE users SET deactivated_at = now() - interval '31 days' WHERE id = ?", adaId);

        purger.purgeDue();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT role FROM conversation_members WHERE conversation_id = ? AND user_id = ?", String.class, group, kenId))
                .isEqualTo("OWNER");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM conversation_members WHERE conversation_id = ? AND user_id = ?", String.class, group, adaId))
                .isEqualTo("LEFT");
    }

    @Test
    void anAccountInsideTheGracePeriodIsLeftAlone() throws Exception {
        requestDeletion(ada, TestUsers.PASSWORD).andExpect(status().isOk());

        assertThat(purger.purgeDue()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT username FROM users WHERE id = ?", String.class, adaId))
                .isEqualTo("ada");
    }

    private ResultActions requestDeletion(String bearer, String password) throws Exception {
        return perform(bearer, post("/users/me/deletion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\": \"" + password + "\"}"));
    }

    private ResultActions login(String username) throws Exception {
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"" + username + "\",\"password\":\"" + TestUsers.PASSWORD + "\"}"));
    }

    private UUID publish(String bearer, String body) throws Exception {
        return UUID.fromString(data(perform(bearer, post("/posts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\": \"" + body + "\"}"))).get("id").asString());
    }

    private ResultActions perform(String bearer, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private JsonNode data(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("data");
    }

    private UUID idOf(String username) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", UUID.class, username);
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }
}
