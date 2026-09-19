package com.application.devhub.post;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

final class PostFixtures {

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper;

    PostFixtures(MockMvc mockMvc, JdbcTemplate jdbcTemplate, JsonMapper jsonMapper) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    ResultActions create(String bearer, String json) throws Exception {
        return mockMvc.perform(post("/posts").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    UUID publish(String bearer, String body) throws Exception {
        return idOf(create(bearer, "{\"body\": \"" + body + "\"}"));
    }

    UUID reply(String bearer, UUID parent, String body) throws Exception {
        return idOf(create(bearer, "{\"body\": \"" + body + "\", \"replyToId\": \"" + parent + "\"}"));
    }

    UUID insert(UUID authorId, String body, int minutesAgo) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO posts (id, author_id, body, created_at) VALUES (?, ?, ?, now() - make_interval(mins => ?))",
                id, authorId, body, minutesAgo);
        return id;
    }

    void follow(UUID follower, UUID followee) {
        jdbcTemplate.update("INSERT INTO follows (follower_id, followee_id) VALUES (?, ?)", follower, followee);
    }

    void topics(UUID userId, String... slugs) {
        for (String slug : slugs) {
            jdbcTemplate.update("INSERT INTO user_topics (user_id, topic_slug) VALUES (?, ?)", userId, slug);
        }
    }

    String nextCursor(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString())
                .get("data").get("nextCursor").asString();
    }

    private UUID idOf(ResultActions result) throws Exception {
        return UUID.fromString(jsonMapper.readTree(result.andReturn().getResponse().getContentAsString())
                .get("data").get("id").asString());
    }
}
