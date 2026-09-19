package com.application.devhub.user;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserSetupIntegrationTest extends IntegrationTest {

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
    void aNewAccountHasNotCompletedSetup() throws Exception {
        me().andExpect(jsonPath("$.data.setupCompleted").value(false));
    }

    @Test
    void setupCannotBeCompletedWithoutTopics() throws Exception {
        completeSetup()
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("TOPICS_REQUIRED"));

        me().andExpect(jsonPath("$.data.setupCompleted").value(false));
    }

    @Test
    void completingSetupIsRememberedAndSafeToRepeat() throws Exception {
        mockMvc.perform(put("/users/me/topics").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"slugs\": [\"rust\"]}"));

        completeSetup().andExpect(status().isOk()).andExpect(jsonPath("$.data.setupCompleted").value(true));
        completeSetup().andExpect(status().isOk());

        me().andExpect(jsonPath("$.data.setupCompleted").value(true));
    }

    private ResultActions completeSetup() throws Exception {
        return mockMvc.perform(put("/users/me/setup-completion").header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private ResultActions me() throws Exception {
        return mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer));
    }
}
