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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommunityIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    private CommunityFixtures communities;
    private UUID ada;
    private String adaBearer;

    @BeforeEach
    void setUp() throws Exception {
        communities = new CommunityFixtures(mockMvc, jdbcTemplate, jsonMapper);
        ada = users.verified("ada");
        communities.age(ada, 30);
        adaBearer = users.bearer("ada");
    }

    @Test
    void theFounderOwnsTheNewCommunityAndIsItsFirstMember() throws Exception {
        communities.create(adaBearer, "rust-lang", "OPEN", "rust", "security")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value("rust-lang"))
                .andExpect(jsonPath("$.data.name").value("rust-lang community"))
                .andExpect(jsonPath("$.data.joinPolicy").value("OPEN"))
                .andExpect(jsonPath("$.data.topics", contains("rust", "security")))
                .andExpect(jsonPath("$.data.memberCount").value(1))
                .andExpect(jsonPath("$.data.viewer.role").value("OWNER"));
    }

    @Test
    void slugsAreUniqueAndWellFormed() throws Exception {
        communities.found(adaBearer, "rust-lang", "OPEN");

        communities.create(adaBearer, "rust-lang", "OPEN")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("COMMUNITY_SLUG_TAKEN"));
        communities.create(adaBearer, "-rust", "OPEN")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors.slug").exists());
        communities.create(adaBearer, "go", "OPEN")
                .andExpect(status().isBadRequest());
    }

    @Test
    void topicsMustBeKnownAndAtMostThree() throws Exception {
        communities.create(adaBearer, "mystery", "OPEN", "not-a-topic")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors.topics").exists());
        communities.create(adaBearer, "polyglot", "OPEN", "rust", "go", "kotlin", "typescript")
                .andExpect(status().isBadRequest());
    }

    @Test
    void newAccountsCantFoundCommunities() throws Exception {
        users.verified("newbie");

        communities.create(users.bearer("newbie"), "fresh", "OPEN")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_TOO_NEW"));
    }

    @Test
    void noOneOwnsMoreThanThreeCommunities() throws Exception {
        communities.found(adaBearer, "first", "OPEN");
        communities.found(adaBearer, "second", "OPEN");
        communities.found(adaBearer, "third", "OPEN");

        communities.create(adaBearer, "fourth", "OPEN")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMUNITY_LIMIT_REACHED"));
    }

    @Test
    void onlyModeratorsCanEditAndOmittedFieldsStayPut() throws Exception {
        communities.found(adaBearer, "rust-lang", "OPEN");
        users.verified("grace");
        String grace = users.bearer("grace");
        communities.join(grace, "rust-lang").andExpect(status().isOk());

        mockMvc.perform(patch("/communities/rust-lang").header(HttpHeaders.AUTHORIZATION, grace)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Hijacked\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_COMMUNITY_MODERATOR"));

        mockMvc.perform(patch("/communities/rust-lang").header(HttpHeaders.AUTHORIZATION, adaBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Rust\", \"joinPolicy\": \"RESTRICTED\", \"description\": \"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Rust"))
                .andExpect(jsonPath("$.data.joinPolicy").value("RESTRICTED"))
                .andExpect(jsonPath("$.data.description").doesNotExist())
                .andExpect(jsonPath("$.data.topics", contains("rust")));
    }

    @Test
    void searchFindsBySlugAndFuzzyNameAndBrowsingListsTheLargest() throws Exception {
        communities.found(adaBearer, "rust-lang", "OPEN");
        communities.found(adaBearer, "golang", "OPEN");
        users.verified("grace");
        communities.join(users.bearer("grace"), "golang");

        mockMvc.perform(get("/communities").param("q", "rust").header(HttpHeaders.AUTHORIZATION, adaBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].slug").value("rust-lang"))
                .andExpect(jsonPath("$.data[0].joined").value(true));
        mockMvc.perform(get("/communities").param("q", "golang communty").header(HttpHeaders.AUTHORIZATION, adaBearer))
                .andExpect(jsonPath("$.data[0].slug").value("golang"));
        mockMvc.perform(get("/communities").header(HttpHeaders.AUTHORIZATION, adaBearer))
                .andExpect(jsonPath("$.data[0].slug").value("golang"))
                .andExpect(jsonPath("$.data[0].memberCount").value(2));
    }

    @Test
    void suggestionsShareTheUsersTopicsAndSkipJoinedOnes() throws Exception {
        communities.create(adaBearer, "rust-lang", "OPEN", "rust").andExpect(status().isCreated());
        communities.create(adaBearer, "gophers", "OPEN", "go").andExpect(status().isCreated());
        communities.create(adaBearer, "systems", "OPEN", "rust", "go").andExpect(status().isCreated());
        UUID grace = users.verified("grace");
        jdbcTemplate.update("INSERT INTO user_topics (user_id, topic_slug) VALUES (?, 'rust'), (?, 'go')", grace, grace);
        String graceBearer = users.bearer("grace");
        communities.join(graceBearer, "gophers");

        mockMvc.perform(get("/communities/suggestions").header(HttpHeaders.AUTHORIZATION, graceBearer))
                .andExpect(jsonPath("$.data[*].slug", contains("systems", "rust-lang")));
        mockMvc.perform(get("/users/me/communities").header(HttpHeaders.AUTHORIZATION, graceBearer))
                .andExpect(jsonPath("$.data[*].slug", contains("gophers")));
    }

    @Test
    void unknownCommunitiesAreNotFound() throws Exception {
        mockMvc.perform(get("/communities/nowhere").header(HttpHeaders.AUTHORIZATION, adaBearer))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/communities/nowhere/posts").header(HttpHeaders.AUTHORIZATION, adaBearer))
                .andExpect(status().isNotFound());
    }
}
