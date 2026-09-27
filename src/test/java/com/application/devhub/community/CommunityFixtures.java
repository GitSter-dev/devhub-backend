package com.application.devhub.community;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

final class CommunityFixtures {

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper;

    CommunityFixtures(MockMvc mockMvc, JdbcTemplate jdbcTemplate, JsonMapper jsonMapper) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    void age(UUID userId, int days) {
        jdbcTemplate.update("UPDATE users SET created_at = now() - make_interval(days => ?) WHERE id = ?", days, userId);
    }

    ResultActions create(String bearer, String slug, String policy, String... topics) throws Exception {
        String topicJson = topics.length == 0 ? "[\"rust\"]" : "[\"" + String.join("\", \"", topics) + "\"]";
        return mockMvc.perform(post("/communities").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"slug": "%s", "name": "%s community", "description": "About %s",
                         "joinPolicy": "%s", "topics": %s}""".formatted(slug, slug, slug, policy, topicJson)));
    }

    UUID found(String bearer, String slug, String policy) throws Exception {
        return idOf(create(bearer, slug, policy).andExpect(status().isCreated()));
    }

    ResultActions join(String bearer, String slug) throws Exception {
        return mockMvc.perform(put("/communities/" + slug + "/membership").header(HttpHeaders.AUTHORIZATION, bearer));
    }

    ResultActions leave(String bearer, String slug) throws Exception {
        return mockMvc.perform(delete("/communities/" + slug + "/membership").header(HttpHeaders.AUTHORIZATION, bearer));
    }

    ResultActions publish(String bearer, UUID communityId, String body) throws Exception {
        return mockMvc.perform(post("/posts").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\": \"" + body + "\", \"communityId\": \"" + communityId + "\"}"));
    }

    UUID idOf(ResultActions result) throws Exception {
        return UUID.fromString(jsonMapper.readTree(result.andReturn().getResponse().getContentAsString())
                .get("data").get("id").asString());
    }
}
