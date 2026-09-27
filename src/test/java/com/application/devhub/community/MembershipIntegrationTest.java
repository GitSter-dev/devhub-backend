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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MembershipIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    private CommunityFixtures communities;
    private String owner;
    private String grace;

    @BeforeEach
    void setUp() throws Exception {
        communities = new CommunityFixtures(mockMvc, jdbcTemplate, jsonMapper);
        communities.age(users.verified("ada"), 30);
        owner = users.bearer("ada");
        users.verified("grace");
        grace = users.bearer("grace");
    }

    @Test
    void openCommunitiesAdmitAtOnceAndJoiningTwiceChangesNothing() throws Exception {
        communities.found(owner, "rust-lang", "OPEN");

        communities.join(grace, "rust-lang")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewer.role").value("MEMBER"))
                .andExpect(jsonPath("$.data.memberCount").value(2));
        communities.join(grace, "rust-lang")
                .andExpect(jsonPath("$.data.memberCount").value(2));

        communities.leave(grace, "rust-lang")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewer.role").doesNotExist())
                .andExpect(jsonPath("$.data.memberCount").value(1));
        communities.leave(grace, "rust-lang")
                .andExpect(jsonPath("$.data.memberCount").value(1));
    }

    @Test
    void restrictedCommunitiesTakeRequestsThatModeratorsDecide() throws Exception {
        communities.found(owner, "inner-circle", "RESTRICTED");

        mockMvc.perform(put("/communities/inner-circle/membership").header(HttpHeaders.AUTHORIZATION, grace)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\": \"I write compilers\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewer.role").doesNotExist())
                .andExpect(jsonPath("$.data.viewer.requested").value(true))
                .andExpect(jsonPath("$.data.memberCount").value(1));
        communities.join(grace, "inner-circle").andExpect(status().isOk());

        mockMvc.perform(get("/communities/inner-circle/join-requests").header(HttpHeaders.AUTHORIZATION, grace))
                .andExpect(status().isForbidden());
        String requests = mockMvc.perform(get("/communities/inner-circle/join-requests")
                        .header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].requester.username").value("grace"))
                .andExpect(jsonPath("$.data.items[0].message").value("I write compilers"))
                .andReturn().getResponse().getContentAsString();
        String requestId = jsonMapper.readTree(requests).get("data").get("items").get(0).get("id").asString();

        mockMvc.perform(post("/communities/inner-circle/join-requests/{id}/approve", requestId)
                        .header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isOk());
        mockMvc.perform(post("/communities/inner-circle/join-requests/{id}/approve", requestId)
                        .header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/communities/inner-circle").header(HttpHeaders.AUTHORIZATION, grace))
                .andExpect(jsonPath("$.data.viewer.role").value("MEMBER"))
                .andExpect(jsonPath("$.data.viewer.requested").value(false))
                .andExpect(jsonPath("$.data.memberCount").value(2));
    }

    @Test
    void declinedRequestersCanAskAgainAndPendingRequestsCanBeWithdrawn() throws Exception {
        communities.found(owner, "inner-circle", "RESTRICTED");
        communities.join(grace, "inner-circle");
        String requestId = jdbcTemplate.queryForObject(
                "SELECT id::text FROM community_join_requests WHERE status = 'PENDING'", String.class);

        mockMvc.perform(post("/communities/inner-circle/join-requests/{id}/decline", requestId)
                        .header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isOk());
        communities.join(grace, "inner-circle")
                .andExpect(jsonPath("$.data.viewer.requested").value(true));

        communities.leave(grace, "inner-circle")
                .andExpect(jsonPath("$.data.viewer.requested").value(false));
    }

    @Test
    void theOwnerCantLeave() throws Exception {
        communities.found(owner, "rust-lang", "OPEN");

        communities.leave(owner, "rust-lang")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("OWNER_CANNOT_LEAVE"));
    }

    @Test
    void membersAreListedLongestStandingFirstAndFilterByRole() throws Exception {
        communities.found(owner, "rust-lang", "OPEN");
        communities.join(grace, "rust-lang");

        mockMvc.perform(get("/communities/rust-lang/members").header(HttpHeaders.AUTHORIZATION, grace))
                .andExpect(jsonPath("$.data.items[*].username", contains("ada", "grace")))
                .andExpect(jsonPath("$.data.items[0].role").value("OWNER"))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
        mockMvc.perform(get("/communities/rust-lang/members").param("role", "MEMBER")
                        .header(HttpHeaders.AUTHORIZATION, grace))
                .andExpect(jsonPath("$.data.items[*].username", contains("grace")));
    }
}
