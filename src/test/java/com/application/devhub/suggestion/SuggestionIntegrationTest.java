package com.application.devhub.suggestion;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SuggestionIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID me;
    private UUID alice;
    private UUID bob;
    private UUID carol;
    private UUID dave;
    private String bearer;

    @BeforeEach
    void seed() throws Exception {
        me = users.verified("me");
        alice = users.verified("alice");
        bob = users.verified("bob");
        carol = users.verified("carol");
        dave = users.verified("dave");
        users.unverified("eve");
        topics(me, "rust", "go");
        topics(alice, "rust", "go");
        topics(bob, "rust");
        topics(carol, "design");
        follow(bob, carol);
        follow(dave, carol);
        bearer = users.bearer("me");
    }

    @Test
    void sharedTopicsRankFirstThenPopularity() throws Exception {
        suggestions(20)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].user.username", contains("alice", "bob", "carol", "dave")))
                .andExpect(jsonPath("$.data[0].reason.type").value("SHARED_TOPICS"))
                .andExpect(jsonPath("$.data[0].reason.topics", contains("go", "rust")))
                .andExpect(jsonPath("$.data[1].reason.topics", contains("rust")))
                .andExpect(jsonPath("$.data[2].reason.type").value("POPULAR"))
                .andExpect(jsonPath("$.data[2].reason.topics", empty()));
    }

    @Test
    void peopleIAlreadyFollowAreLeftOut() throws Exception {
        follow(me, alice);

        suggestions(20).andExpect(jsonPath("$.data[*].user.username", contains("bob", "carol", "dave")));
    }

    @Test
    void theLimitIsClampedToAtLeastOne() throws Exception {
        suggestions(0).andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].user.username").value("alice"));
    }

    private ResultActions suggestions(int limit) throws Exception {
        return mockMvc.perform(get("/users/me/suggestions").param("limit", String.valueOf(limit))
                .header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private void topics(UUID userId, String... slugs) {
        for (String slug : slugs) {
            jdbcTemplate.update("INSERT INTO user_topics (user_id, topic_slug) VALUES (?, ?)", userId, slug);
        }
    }

    private void follow(UUID follower, UUID followee) {
        jdbcTemplate.update("INSERT INTO follows (follower_id, followee_id) VALUES (?, ?)", follower, followee);
    }
}
