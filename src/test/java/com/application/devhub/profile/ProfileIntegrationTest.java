package com.application.devhub.profile;

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

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID ada;
    private UUID grace;
    private String bearer;

    @BeforeEach
    void seed() throws Exception {
        ada = users.verified("ada", "Ada Lovelace");
        grace = users.verified("grace", "Grace Hopper");
        UUID linus = users.verified("linus");
        follow(ada, grace);
        follow(grace, ada);
        follow(linus, grace);
        jdbcTemplate.update("INSERT INTO user_topics (user_id, topic_slug) VALUES (?, 'rust'), (?, 'go')", grace, grace);
        bearer = users.bearer("ada");
    }

    @Test
    void anotherDevelopersProfileShowsCountsTopicsAndRelationship() throws Exception {
        profile("GRACE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("grace"))
                .andExpect(jsonPath("$.data.displayName").value("Grace Hopper"))
                .andExpect(jsonPath("$.data.topics", contains("go", "rust")))
                .andExpect(jsonPath("$.data.followerCount").value(2))
                .andExpect(jsonPath("$.data.followingCount").value(1))
                .andExpect(jsonPath("$.data.me").value(false))
                .andExpect(jsonPath("$.data.following").value(true))
                .andExpect(jsonPath("$.data.followsYou").value(true));
    }

    @Test
    void myOwnProfileIsMarkedAsMine() throws Exception {
        profile("ada")
                .andExpect(jsonPath("$.data.me").value(true))
                .andExpect(jsonPath("$.data.following").value(false))
                .andExpect(jsonPath("$.data.followsYou").value(false));
    }

    @Test
    void unknownAndUnverifiedAccountsAreNotFound() throws Exception {
        users.unverified("ghost");

        profile("nobody").andExpect(status().isNotFound());
        profile("ghost").andExpect(status().isNotFound());
    }

    @Test
    void editingTheProfileSavesAndTrimsEveryField() throws Exception {
        update("""
                {"displayName": "  Ada L.  ", "bio": "  Rust and Go.  ", "githubUsername": "ada-dev",
                 "websiteUrl": "https://ada.dev"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Ada L."))
                .andExpect(jsonPath("$.data.bio").value("Rust and Go."))
                .andExpect(jsonPath("$.data.githubUsername").value("ada-dev"))
                .andExpect(jsonPath("$.data.websiteUrl").value("https://ada.dev"));

        update("{\"displayName\": \"Ada\", \"bio\": \"   \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bio").doesNotExist())
                .andExpect(jsonPath("$.data.githubUsername").doesNotExist())
                .andExpect(jsonPath("$.data.websiteUrl").doesNotExist());
    }

    @Test
    void invalidFieldsAreRejectedPerField() throws Exception {
        update("""
                {"displayName": " ", "bio": "%s", "githubUsername": "-bad-", "websiteUrl": "http://ada.dev"}
                """.formatted("x".repeat(161)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.fieldErrors.displayName").exists())
                .andExpect(jsonPath("$.error.fieldErrors.bio").exists())
                .andExpect(jsonPath("$.error.fieldErrors.githubUsername").exists())
                .andExpect(jsonPath("$.error.fieldErrors.websiteUrl").value("must be an https:// link"));
    }

    private ResultActions profile(String username) throws Exception {
        return mockMvc.perform(get("/users/{username}/profile", username).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private ResultActions update(String body) throws Exception {
        return mockMvc.perform(put("/users/me/profile").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void follow(UUID follower, UUID followee) {
        jdbcTemplate.update("INSERT INTO follows (follower_id, followee_id) VALUES (?, ?)", follower, followee);
    }
}
