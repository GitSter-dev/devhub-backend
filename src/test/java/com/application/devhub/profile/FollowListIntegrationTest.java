package com.application.devhub.profile;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FollowListIntegrationTest extends IntegrationTest {

    private static final int FOLLOWERS = 25;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    private String bearer;

    @BeforeEach
    void seed() throws Exception {
        UUID viewer = users.verified("viewer");
        UUID star = users.verified("star");
        for (int i = 0; i < FOLLOWERS; i++) {
            UUID fan = users.verified("fan%02d".formatted(i));
            jdbcTemplate.update("""
                    INSERT INTO follows (follower_id, followee_id, created_at)
                    VALUES (?, ?, now() - make_interval(mins => ?))
                    """, fan, star, FOLLOWERS - i);
        }
        jdbcTemplate.update("INSERT INTO follows (follower_id, followee_id) SELECT ?, id FROM users WHERE username = 'fan24'",
                viewer);
        bearer = users.bearer("viewer");
    }

    @Test
    void followersComeNewestFirstInPagesOfTwenty() throws Exception {
        String response = list("star", "followers", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(20)))
                .andExpect(jsonPath("$.data.items[0].username").value("fan24"))
                .andExpect(jsonPath("$.data.items[0].following").value(true))
                .andExpect(jsonPath("$.data.items[1].following").value(false))
                .andExpect(jsonPath("$.data.nextCursor").exists())
                .andReturn().getResponse().getContentAsString();
        String cursor = jsonMapper.readTree(response).get("data").get("nextCursor").asString();

        list("star", "followers", cursor)
                .andExpect(jsonPath("$.data.items[*].username", contains("fan04", "fan03", "fan02", "fan01", "fan00")))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
    }

    @Test
    void followingListsWhoTheDeveloperFollows() throws Exception {
        list("viewer", "following", null)
                .andExpect(jsonPath("$.data.items[*].username", contains("fan24")))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
    }

    @Test
    void aTamperedCursorIsABadRequest() throws Exception {
        list("star", "followers", "not-a-cursor").andExpect(status().isBadRequest());
    }

    @Test
    void listsOfUnknownDevelopersAreNotFound() throws Exception {
        list("nobody", "followers", null).andExpect(status().isNotFound());
    }

    private ResultActions list(String username, String which, String cursor) throws Exception {
        var request = get("/users/{username}/" + which, username).header(HttpHeaders.AUTHORIZATION, bearer);
        return mockMvc.perform(cursor == null ? request : request.param("cursor", cursor));
    }
}
