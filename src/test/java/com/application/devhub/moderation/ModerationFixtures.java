package com.application.devhub.moderation;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

final class ModerationFixtures {

    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper;
    private final JdbcTemplate jdbcTemplate;

    ModerationFixtures(MockMvc mockMvc, JsonMapper jsonMapper, JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.jsonMapper = jsonMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    ResultActions report(String bearer, String targetType, UUID targetId, String reason) throws Exception {
        return perform(bearer, post("/reports").contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetType\": \"%s\", \"targetId\": \"%s\", \"reason\": \"%s\"}"
                        .formatted(targetType, targetId, reason)));
    }

    UUID publish(String bearer, String body) throws Exception {
        return UUID.fromString(data(perform(bearer, post("/posts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\": \"" + body + "\"}")).andExpect(status().isCreated())).get("id").asString());
    }

    JsonNode cases(String bearer, String status) throws Exception {
        return data(perform(bearer, get("/admin/moderation/cases").param("status", status))
                .andExpect(status().isOk())).get("items");
    }

    JsonNode caseDetail(String bearer, UUID caseId) throws Exception {
        return data(perform(bearer, get("/admin/moderation/cases/{id}", caseId)).andExpect(status().isOk()));
    }

    ResultActions act(String bearer, UUID caseId, String action, Integer days) throws Exception {
        String body = days == null ? "{\"action\": \"%s\"}".formatted(action)
                : "{\"action\": \"%s\", \"days\": %d}".formatted(action, days);
        return perform(bearer, post("/admin/moderation/cases/{id}/actions", caseId)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    UUID onlyCaseId() {
        return jdbcTemplate.queryForObject("SELECT id FROM moderation_cases", UUID.class);
    }

    void ageAccount(UUID userId, int days) {
        jdbcTemplate.update("UPDATE users SET created_at = now() - make_interval(days => ?) WHERE id = ?", days, userId);
    }

    ResultActions perform(String bearer, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer));
    }

    JsonNode data(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("data");
    }
}
