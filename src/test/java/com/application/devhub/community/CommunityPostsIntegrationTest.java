package com.application.devhub.community;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommunityPostsIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    private CommunityFixtures communities;
    private String ada;
    private String grace;
    private String linus;
    private UUID rust;

    @BeforeEach
    void setUp() throws Exception {
        communities = new CommunityFixtures(mockMvc, jdbcTemplate, jsonMapper);
        communities.age(users.verified("ada"), 30);
        ada = users.bearer("ada");
        users.verified("grace");
        grace = users.bearer("grace");
        users.verified("linus");
        linus = users.bearer("linus");
        rust = communities.found(ada, "rust-lang", "OPEN");
    }

    @Test
    void membersPostIntoTheCommunityAndThePostSaysWhere() throws Exception {
        communities.join(grace, "rust-lang");

        communities.publish(grace, rust, "Borrow checker finally clicked")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.community.slug").value("rust-lang"))
                .andExpect(jsonPath("$.data.community.name").value("rust-lang community"));
    }

    @Test
    void nonMembersCantPostButCanRead() throws Exception {
        communities.publish(ada, rust, "Welcome, everyone").andExpect(status().isCreated());

        communities.publish(grace, rust, "Let me in")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_COMMUNITY_MEMBER"));
        mockMvc.perform(get("/communities/rust-lang/posts").header(HttpHeaders.AUTHORIZATION, grace))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].body", contains("Welcome, everyone")));
    }

    @Test
    void repliesStayInTheirThreadsCommunityAndNeedMembership() throws Exception {
        UUID root = communities.idOf(communities.publish(ada, rust, "Favourite crate?"));

        String reply = "{\"body\": \"%s\", \"replyToId\": \"" + root + "\"}";
        mockMvc.perform(post("/posts").header(HttpHeaders.AUTHORIZATION, grace)
                        .contentType(MediaType.APPLICATION_JSON).content(reply.formatted("serde")))
                .andExpect(status().isForbidden());
        communities.join(grace, "rust-lang");
        mockMvc.perform(post("/posts").header(HttpHeaders.AUTHORIZATION, grace)
                        .contentType(MediaType.APPLICATION_JSON).content(reply.formatted("serde")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.community.slug").value("rust-lang"));

        mockMvc.perform(get("/communities/rust-lang/posts").header(HttpHeaders.AUTHORIZATION, ada))
                .andExpect(jsonPath("$.data.items[*].body", contains("Favourite crate?")));
    }

    @Test
    void joinedCommunitiesFeedTheHomeFeedAheadOfTopicMatches() throws Exception {
        communities.publish(ada, rust, "Rust 2027 edition is out").andExpect(status().isCreated());
        mockMvc.perform(get("/feed").header(HttpHeaders.AUTHORIZATION, linus))
                .andExpect(jsonPath("$.data.items").isEmpty());

        communities.join(linus, "rust-lang");

        mockMvc.perform(get("/feed").header(HttpHeaders.AUTHORIZATION, linus))
                .andExpect(jsonPath("$.data.items[*].body", contains("Rust 2027 edition is out")))
                .andExpect(jsonPath("$.data.items[0].community.slug").value("rust-lang"));
    }

    @Test
    void postsOutsideCommunitiesHaveNoCommunity() throws Exception {
        mockMvc.perform(post("/posts").header(HttpHeaders.AUTHORIZATION, grace)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\": \"Just me\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.community").doesNotExist());
    }

    @Test
    void postingIntoAnUnknownCommunityIsNotFound() throws Exception {
        communities.publish(grace, UUID.randomUUID(), "Hello?")
                .andExpect(status().isNotFound());
    }
}
