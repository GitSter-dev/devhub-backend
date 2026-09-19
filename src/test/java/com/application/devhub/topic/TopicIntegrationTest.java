package com.application.devhub.topic;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TopicIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers users;

    private String bearer;

    @BeforeEach
    void signIn() throws Exception {
        users.verified("ada");
        bearer = users.bearer("ada");
    }

    @Test
    void listsTheWholeCatalogSortedByName() throws Exception {
        mockMvc.perform(get("/topics").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(12)))
                .andExpect(jsonPath("$.data[0].slug").value("ai"))
                .andExpect(jsonPath("$.data[0].name").value("AI"));
    }

    @Test
    void theCatalogRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/topics")).andExpect(status().isUnauthorized());
    }

    @Test
    void replacesTheUsersTopicsIgnoringDuplicates() throws Exception {
        replace("[\"rust\", \"go\", \"rust\"]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slugs", contains("go", "rust")));

        replace("[\"ai\"]").andExpect(jsonPath("$.data.slugs", contains("ai")));

        mockMvc.perform(get("/users/me/topics").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slugs", contains("ai")));
    }

    @Test
    void anEmptyListIsRejected() throws Exception {
        assertSlugsRejected(replace("[]"));
    }

    @Test
    void moreThanTenTopicsAreRejected() throws Exception {
        assertSlugsRejected(replace("""
                ["ai", "databases", "design", "devops", "game-dev", "go", "kotlin", "open-source", "react-native",
                 "rust", "security"]
                """));
    }

    @Test
    void anUnknownTopicIsRejectedAndNothingChanges() throws Exception {
        replace("[\"rust\"]");

        assertSlugsRejected(replace("[\"rust\", \"cobol\"]"))
                .andExpect(jsonPath("$.error.fieldErrors.slugs").value("contains an unknown topic"));

        mockMvc.perform(get("/users/me/topics").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.data.slugs", contains("rust")));
    }

    private ResultActions replace(String slugs) throws Exception {
        return mockMvc.perform(put("/users/me/topics").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slugs\": " + slugs + "}"));
    }

    private static ResultActions assertSlugsRejected(ResultActions result) throws Exception {
        return result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.fieldErrors.slugs").exists());
    }
}
