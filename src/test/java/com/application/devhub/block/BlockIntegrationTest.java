package com.application.devhub.block;

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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BlockIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestUsers users;

    private UUID adaId;
    private UUID kenId;
    private String ada;
    private String ken;

    @BeforeEach
    void setUp() throws Exception {
        adaId = users.verified("ada", "Ada");
        kenId = users.verified("ken", "Ken");
        ada = users.bearer("ada");
        ken = users.bearer("ken");
    }

    @Test
    void blockingHidesProfilesAndPostsBothWays() throws Exception {
        UUID adaPost = publish(ada, "Ada writes");
        UUID kenPost = publish(ken, "Ken writes");
        follow(ada, kenId);
        follow(ken, adaId);

        block(ada, kenId).andExpect(status().isOk());

        perform(ken, get("/users/ada/profile")).andExpect(status().isNotFound());
        perform(ada, get("/users/ken/profile")).andExpect(status().isNotFound());
        perform(ken, get("/users/ada/posts")).andExpect(status().isNotFound());
        perform(ken, get("/posts/{id}", adaPost)).andExpect(status().isNotFound());
        perform(ada, get("/posts/{id}", kenPost)).andExpect(status().isNotFound());
        assertThat(ids(data(perform(ada, get("/feed"))).get("items"))).doesNotContain(kenPost.toString());
        assertThat(ids(data(perform(ken, get("/feed"))).get("items"))).doesNotContain(adaPost.toString());
        assertThat(ids(data(perform(ken, get("/users/search").param("q", "ada"))))).isEmpty();
    }

    @Test
    void blockingRemovesFollowsBothWaysAndPreventsNewOnes() throws Exception {
        follow(ada, kenId);
        follow(ken, adaId);

        block(ada, kenId);

        assertThat(count("SELECT count(*) FROM follows")).isZero();
        perform(ken, put("/users/me/following/{id}", adaId)).andExpect(status().isNotFound());
        perform(ada, put("/users/me/following/{id}", kenId)).andExpect(status().isNotFound());
    }

    @Test
    void aBlockedPersonCannotLikeOrReply() throws Exception {
        UUID post = publish(ada, "Ada writes");
        block(ada, kenId);

        perform(ken, put("/posts/{id}/like", post)).andExpect(status().isNotFound());
        perform(ken, post("/posts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\": \"reply\", \"replyToId\": \"" + post + "\"}")).andExpect(status().isNotFound());
    }

    @Test
    void theDirectConversationDisappearsAndCannotBeReopenedOrUsed() throws Exception {
        follow(ada, kenId);
        UUID conversation = UUID.fromString(data(openDirect(ken, adaId)).get("id").asString());
        send(ken, conversation).andExpect(status().isCreated());

        block(ken, adaId);

        assertThat(data(perform(ada, get("/conversations"))).get("items")).isEmpty();
        perform(ada, get("/conversations/{id}", conversation)).andExpect(status().isNotFound());
        send(ada, conversation).andExpect(status().isNotFound());
        openDirect(ada, kenId).andExpect(status().isNotFound());
    }

    @Test
    void notificationsBetweenThemAreWithdrawn() throws Exception {
        follow(ken, adaId);
        jdbcTemplate.update("""
                INSERT INTO notifications (id, recipient_id, type, group_key) VALUES (gen_random_uuid(), ?, 'NEW_FOLLOWER', 'NEW_FOLLOWER')
                """, adaId);
        jdbcTemplate.update("INSERT INTO notification_actors (notification_id, actor_id, subject_id) SELECT id, ?, ? FROM notifications",
                kenId, kenId);

        block(ada, kenId);

        assertThat(count("SELECT count(*) FROM notification_actors")).isZero();
    }

    @Test
    void unblockingRestoresVisibilityButNotFollows() throws Exception {
        follow(ada, kenId);
        block(ada, kenId);

        perform(ada, delete("/users/me/blocks/{id}", kenId)).andExpect(status().isOk());

        perform(ada, get("/users/ken/profile")).andExpect(status().isOk());
        assertThat(count("SELECT count(*) FROM follows")).isZero();
    }

    @Test
    void theBlockListShowsWhoIBlocked() throws Exception {
        UUID linusId = users.verified("linus", "Linus");
        block(ada, kenId);
        block(ada, linusId);
        block(ada, kenId).andExpect(status().isOk());

        JsonNode page = data(perform(ada, get("/users/me/blocks")));
        assertThat(page.get("items").valueStream().map(item -> item.get("username").asString()).toList())
                .containsExactly("linus", "ken");
        assertThat(data(perform(ken, get("/users/me/blocks"))).get("items")).isEmpty();
    }

    @Test
    void blockingYourselfOrAnUnknownUserFails() throws Exception {
        block(ada, adaId).andExpect(status().isBadRequest());
        block(ada, UUID.randomUUID()).andExpect(status().isNotFound());
    }

    private ResultActions block(String bearer, UUID userId) throws Exception {
        return perform(bearer, put("/users/me/blocks/{id}", userId));
    }

    private void follow(String bearer, UUID userId) throws Exception {
        perform(bearer, put("/users/me/following/{id}", userId)).andExpect(status().isOk());
    }

    private UUID publish(String bearer, String body) throws Exception {
        return UUID.fromString(data(perform(bearer, post("/posts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\": \"" + body + "\"}"))).get("id").asString());
    }

    private ResultActions openDirect(String bearer, UUID userId) throws Exception {
        return perform(bearer, post("/conversations/direct").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\": \"" + userId + "\"}"));
    }

    private ResultActions send(String bearer, UUID conversationId) throws Exception {
        return perform(bearer, post("/conversations/{id}/messages", conversationId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"clientMessageId\": \"" + UUID.randomUUID() + "\", \"body\": \"hi\"}"));
    }

    private ResultActions perform(String bearer,
                                  MockHttpServletRequestBuilder request)
            throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private JsonNode data(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("data");
    }

    private static List<String> ids(JsonNode items) {
        return items.valueStream().map(item -> item.get("id").asString()).toList();
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
