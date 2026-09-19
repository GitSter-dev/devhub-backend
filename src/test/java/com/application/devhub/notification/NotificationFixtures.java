package com.application.devhub.notification;

import com.application.devhub.outbox.OutboxRelay;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

final class NotificationFixtures {

    private static final int MAX_RELAY_ROUNDS = 50;

    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper;
    private final JdbcTemplate jdbcTemplate;
    private final OutboxRelay outboxRelay;

    NotificationFixtures(MockMvc mockMvc, JsonMapper jsonMapper, JdbcTemplate jdbcTemplate, OutboxRelay outboxRelay) {
        this.mockMvc = mockMvc;
        this.jsonMapper = jsonMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.outboxRelay = outboxRelay;
    }

    UUID publish(String bearer, String body) throws Exception {
        return idOf(mockMvc.perform(post("/posts").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\": \"" + body + "\"}")));
    }

    UUID reply(String bearer, UUID parentId, String body) throws Exception {
        return idOf(mockMvc.perform(post("/posts").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\": \"" + body + "\", \"replyToId\": \"" + parentId + "\"}")));
    }

    void deletePost(String bearer, UUID postId) throws Exception {
        mockMvc.perform(delete("/posts/{id}", postId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().is2xxSuccessful());
    }

    void like(String bearer, UUID postId) throws Exception {
        mockMvc.perform(put("/posts/{id}/like", postId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().is2xxSuccessful());
    }

    void unlike(String bearer, UUID postId) throws Exception {
        mockMvc.perform(delete("/posts/{id}/like", postId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().is2xxSuccessful());
    }

    void follow(String bearer, UUID userId) throws Exception {
        mockMvc.perform(put("/users/me/following/{id}", userId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().is2xxSuccessful());
    }

    void unfollow(String bearer, UUID userId) throws Exception {
        mockMvc.perform(delete("/users/me/following/{id}", userId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().is2xxSuccessful());
    }

    UUID openDirect(String bearer, UUID userId) throws Exception {
        return idOf(mockMvc.perform(post("/conversations/direct").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"userId\": \"" + userId + "\"}")));
    }

    void send(String bearer, UUID conversationId, String body) throws Exception {
        mockMvc.perform(post("/conversations/{id}/messages", conversationId).header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientMessageId\": \"" + UUID.randomUUID() + "\", \"body\": \"" + body + "\"}"))
                .andExpect(status().isCreated());
    }

    void accept(String bearer, UUID conversationId) throws Exception {
        mockMvc.perform(post("/conversations/{id}/accept", conversationId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().is2xxSuccessful());
    }

    ResultActions list(String bearer) throws Exception {
        return mockMvc.perform(get("/notifications").header(HttpHeaders.AUTHORIZATION, bearer));
    }

    JsonNode items(String bearer) throws Exception {
        return data(list(bearer).andExpect(status().isOk())).get("items");
    }

    long unseen(String bearer) throws Exception {
        return data(mockMvc.perform(get("/notifications/unseen-count").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())).get("count").asLong();
    }

    ResultActions markSeen(String bearer, String until) throws Exception {
        return mockMvc.perform(post("/notifications/seen").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"until\": \"" + until + "\"}"));
    }

    void drain() {
        for (int round = 0; round < MAX_RELAY_ROUNDS && pending() > 0; round++) {
            outboxRelay.relayPending();
        }
    }

    private long pending() {
        Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_events WHERE status = 'PENDING'", Long.class);
        return count == null ? 0 : count;
    }

    private UUID idOf(ResultActions result) throws Exception {
        return UUID.fromString(data(result.andExpect(status().is2xxSuccessful())).get("id").asString());
    }

    private JsonNode data(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("data");
    }
}
