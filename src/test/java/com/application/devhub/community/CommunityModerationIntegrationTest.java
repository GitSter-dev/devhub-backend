package com.application.devhub.community;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import com.application.devhub.account.AccountPurger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommunityModerationIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private AccountPurger purger;

    private CommunityFixtures communities;
    private UUID adaId;
    private UUID graceId;
    private UUID linusId;
    private String ada;
    private String grace;
    private String linus;
    private UUID rust;

    @BeforeEach
    void setUp() throws Exception {
        communities = new CommunityFixtures(mockMvc, jdbcTemplate, jsonMapper);
        adaId = users.verified("ada");
        communities.age(adaId, 30);
        ada = users.bearer("ada");
        graceId = users.verified("grace");
        grace = users.bearer("grace");
        linusId = users.verified("linus");
        linus = users.bearer("linus");
        rust = communities.found(ada, "rust-lang", "OPEN");
        communities.join(grace, "rust-lang");
        communities.join(linus, "rust-lang");
    }

    @Test
    void theOwnerAppointsModeratorsAndCanHandTheCommunityOver() throws Exception {
        perform(put("/communities/rust-lang/moderators/{id}", linusId), grace)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_COMMUNITY_OWNER"));
        perform(put("/communities/rust-lang/moderators/{id}", graceId), ada).andExpect(status().isOk());

        perform(post("/communities/rust-lang/ownership").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\": \"" + graceId + "\"}"), ada).andExpect(status().isOk());

        perform(get("/communities/rust-lang"), grace).andExpect(jsonPath("$.data.viewer.role").value("OWNER"));
        perform(get("/communities/rust-lang"), ada).andExpect(jsonPath("$.data.viewer.role").value("MODERATOR"));
        communities.leave(ada, "rust-lang").andExpect(status().isOk());
        perform(delete("/communities/rust-lang/moderators/{id}", linusId), ada)
                .andExpect(jsonPath("$.error.code").value("NOT_COMMUNITY_OWNER"));
    }

    @Test
    void moderatorsWriteTheRulesEveryoneReads() throws Exception {
        String rules = """
                {"rules": [{"title": "Be kind", "body": "Critique code, not people."}, {"title": "Stay on topic"}]}""";
        perform(put("/communities/rust-lang/rules").contentType(MediaType.APPLICATION_JSON).content(rules), grace)
                .andExpect(status().isForbidden());
        perform(put("/communities/rust-lang/rules").contentType(MediaType.APPLICATION_JSON).content(rules), ada)
                .andExpect(status().isOk());

        perform(get("/communities/rust-lang"), grace)
                .andExpect(jsonPath("$.data.rules[*].title", contains("Be kind", "Stay on topic")))
                .andExpect(jsonPath("$.data.rules[0].body").value("Critique code, not people."));
    }

    @Test
    void slowModeMakesMembersWaitButNotModerators() throws Exception {
        perform(patch("/communities/rust-lang").contentType(MediaType.APPLICATION_JSON)
                .content("{\"slowModeSeconds\": 600}"), ada)
                .andExpect(jsonPath("$.data.slowModeSeconds").value(600));

        communities.publish(grace, rust, "First").andExpect(status().isCreated());
        communities.publish(grace, rust, "Second")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("SLOW_MODE_ACTIVE"))
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
        communities.publish(ada, rust, "Mod one").andExpect(status().isCreated());
        communities.publish(ada, rust, "Mod two").andExpect(status().isCreated());
    }

    @Test
    void moderatorsPinUpToThreePosts() throws Exception {
        UUID[] posts = new UUID[4];
        for (int i = 0; i < posts.length; i++) {
            posts[i] = communities.idOf(communities.publish(ada, rust, "Post " + i));
        }
        perform(put("/communities/rust-lang/pins/{id}", posts[0]), grace).andExpect(status().isForbidden());
        for (int i = 0; i < 3; i++) {
            perform(put("/communities/rust-lang/pins/{id}", posts[i]), ada).andExpect(status().isOk());
        }
        perform(put("/communities/rust-lang/pins/{id}", posts[3]), ada)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PIN_LIMIT_REACHED"));

        perform(get("/communities/rust-lang/pins"), grace)
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].pinned").value(true));
        perform(delete("/communities/rust-lang/pins/{id}", posts[0]), ada).andExpect(status().isOk());
        perform(put("/communities/rust-lang/pins/{id}", posts[3]), ada).andExpect(status().isOk());
    }

    @Test
    void removalsHideThePostTellTheAuthorAndShowInThePublicLog() throws Exception {
        UUID post = communities.idOf(communities.publish(grace, rust, "Buy cheap crypto"));

        perform(put("/communities/rust-lang/posts/{id}/removal", post).contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\": \"Spam\"}"), ada).andExpect(status().isOk());

        perform(get("/communities/rust-lang/posts"), linus).andExpect(jsonPath("$.data.items").isEmpty());
        currentApp(get("/notifications"), grace)
                .andExpect(jsonPath("$.data.items[0].type").value("COMMUNITY_POST_REMOVED"))
                .andExpect(jsonPath("$.data.items[0].community.slug").value("rust-lang"));
        perform(get("/communities/rust-lang/moderation-log"), linus)
                .andExpect(jsonPath("$.data.items[0].action").value("REMOVE_CONTENT"))
                .andExpect(jsonPath("$.data.items[0].moderatorUsername").value("ada"))
                .andExpect(jsonPath("$.data.items[0].targetUsername").value("grace"))
                .andExpect(jsonPath("$.data.items[0].note").value("Spam"))
                .andExpect(jsonPath("$.data.items[0].body").doesNotExist());

        perform(delete("/communities/rust-lang/posts/{id}/removal", post), ada).andExpect(status().isOk());
        perform(get("/communities/rust-lang/posts"), linus).andExpect(jsonPath("$.data.items", hasSize(1)));
    }

    @Test
    void communityModeratorsCantUndoAPlatformRemoval() throws Exception {
        UUID post = communities.idOf(communities.publish(grace, rust, "Something awful"));
        jdbcTemplate.update("UPDATE posts SET removed_at = now(), removal_scope = 'PLATFORM' WHERE id = ?", post);

        perform(delete("/communities/rust-lang/posts/{id}/removal", post), ada).andExpect(status().isForbidden());
    }

    @Test
    void bannedPeopleLeaveAndCantComeBackUntilUnbanned() throws Exception {
        perform(put("/communities/rust-lang/bans/{id}", graceId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"Harassment\", \"days\": 7}"), ada).andExpect(status().isOk());

        perform(get("/communities/rust-lang"), grace)
                .andExpect(jsonPath("$.data.viewer.role").doesNotExist())
                .andExpect(jsonPath("$.data.viewer.banned").value(true))
                .andExpect(jsonPath("$.data.memberCount").value(2));
        communities.join(grace, "rust-lang")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("BANNED_FROM_COMMUNITY"));
        communities.publish(grace, rust, "Let me back").andExpect(status().isForbidden());
        perform(get("/communities/rust-lang/bans"), ada)
                .andExpect(jsonPath("$.data[0].username").value("grace"))
                .andExpect(jsonPath("$.data[0].reason").value("Harassment"));

        perform(delete("/communities/rust-lang/bans/{id}", graceId), ada).andExpect(status().isOk());
        communities.join(grace, "rust-lang").andExpect(status().isOk());
        perform(get("/communities/rust-lang/moderation-log"), grace)
                .andExpect(jsonPath("$.data.items[*].action", contains("COMMUNITY_UNBAN", "COMMUNITY_BAN")));
    }

    @Test
    void moderatorsCantBeBanned() throws Exception {
        perform(put("/communities/rust-lang/moderators/{id}", graceId), ada);

        perform(put("/communities/rust-lang/bans/{id}", graceId), ada).andExpect(status().isForbidden());
        perform(put("/communities/rust-lang/bans/{id}", adaId), ada).andExpect(status().isForbidden());
    }

    @Test
    void reportsOnCommunityPostsReachItsModeratorsAndDevHubs() throws Exception {
        UUID post = communities.idOf(communities.publish(grace, rust, "Spam spam spam"));
        report(linus, "POST", post).andExpect(status().isOk());

        String caseId = jsonMapper.readTree(perform(get("/communities/rust-lang/moderation/cases"), ada)
                        .andExpect(jsonPath("$.data.items", hasSize(1)))
                        .andExpect(jsonPath("$.data.items[0].community.slug").value("rust-lang"))
                        .andReturn().getResponse().getContentAsString())
                .get("data").get("items").get(0).get("id").asString();
        perform(get("/communities/rust-lang/moderation/cases"), grace).andExpect(status().isForbidden());
        users.admin("root");
        perform(get("/admin/moderation/cases").param("communityId", rust.toString()), users.bearer("root"))
                .andExpect(jsonPath("$.data.items[*].id", contains(caseId)));

        perform(post("/communities/rust-lang/moderation/cases/{id}/actions", caseId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\": \"BAN_AUTHOR\", \"note\": \"Spam\", \"days\": 30}"), ada)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.moderationCase.status").value("ACTIONED"));

        communities.join(grace, "rust-lang").andExpect(jsonPath("$.error.code").value("BANNED_FROM_COMMUNITY"));
        currentApp(get("/notifications"), linus)
                .andExpect(jsonPath("$.data.items[*].type", hasItem("REPORT_RESOLVED")));
    }

    @Test
    void devHubTakesDownAReportedCommunity() throws Exception {
        communities.publish(ada, rust, "Hello").andExpect(status().isCreated());
        report(grace, "COMMUNITY", rust).andExpect(status().isOk());
        users.admin("root");
        String root = users.bearer("root");
        String caseId = jdbcTemplate.queryForObject(
                "SELECT id::text FROM moderation_cases WHERE target_type = 'COMMUNITY'", String.class);

        perform(get("/admin/moderation/cases").param("communityId", rust.toString()), root)
                .andExpect(jsonPath("$.data.items[*].id", contains(caseId)))
                .andExpect(jsonPath("$.data.items[0].community.slug").value("rust-lang"));
        perform(get("/communities/rust-lang/moderation/cases"), ada)
                .andExpect(jsonPath("$.data.items").isEmpty());

        perform(post("/admin/moderation/cases/{id}/actions", caseId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\": \"REMOVE_CONTENT\"}"), root).andExpect(status().isOk());

        perform(get("/admin/moderation/actions"), root)
                .andExpect(jsonPath("$.data.items[0].community.slug").value("rust-lang"));
        perform(get("/communities/rust-lang"), grace).andExpect(status().isNotFound());
        perform(get("/feed"), grace).andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void restrictedCommunitiesTellModeratorsAboutRequestsAndRequestersAboutApproval() throws Exception {
        communities.found(ada, "inner-circle", "RESTRICTED");
        UUID newcomerId = users.verified("newcomer");
        String newcomer = users.bearer("newcomer");
        communities.join(newcomer, "inner-circle");

        currentApp(get("/notifications"), ada)
                .andExpect(jsonPath("$.data.items[0].type").value("COMMUNITY_JOIN_REQUESTED"))
                .andExpect(jsonPath("$.data.items[0].community.slug").value("inner-circle"))
                .andExpect(jsonPath("$.data.items[0].actors[0].username").value("newcomer"));
        String requestId = jdbcTemplate.queryForObject(
                "SELECT id::text FROM community_join_requests WHERE user_id = ?", String.class, newcomerId);
        perform(post("/communities/inner-circle/join-requests/{id}/approve", requestId), ada).andExpect(status().isOk());

        currentApp(get("/notifications"), newcomer)
                .andExpect(jsonPath("$.data.items[0].type").value("COMMUNITY_JOIN_APPROVED"));
    }

    @Test
    void appsFromBeforeCommunitiesNeverSeeCommunityNotifications() throws Exception {
        UUID post = communities.idOf(communities.publish(grace, rust, "Off topic"));
        perform(put("/communities/rust-lang/posts/{id}/removal", post), ada).andExpect(status().isOk());

        perform(get("/notifications"), grace)
                .andExpect(jsonPath("$.data.items[*].type", not(hasItem("COMMUNITY_POST_REMOVED"))));
        perform(get("/notifications/unseen-count"), grace).andExpect(jsonPath("$.data.count").value(0));
        currentApp(get("/notifications/unseen-count"), grace).andExpect(jsonPath("$.data.count").value(1));
    }

    @Test
    void deletingTheOwnersAccountHandsTheCommunityToAModerator() throws Exception {
        perform(put("/communities/rust-lang/moderators/{id}", linusId), ada);
        jdbcTemplate.update("UPDATE users SET deactivated_at = now() - interval '31 days' WHERE id = ?", adaId);

        purger.purgeDue();

        perform(get("/communities/rust-lang"), linus)
                .andExpect(jsonPath("$.data.viewer.role").value("OWNER"))
                .andExpect(jsonPath("$.data.memberCount").value(2));
    }

    private ResultActions report(String bearer, String type, UUID target) throws Exception {
        return perform(post("/reports").contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetType\": \"%s\", \"targetId\": \"%s\", \"reason\": \"SPAM\"}".formatted(type, target)),
                bearer);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String bearer) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private ResultActions currentApp(MockHttpServletRequestBuilder request, String bearer) throws Exception {
        return perform(request.header("X-App-Platform", "android").header("X-App-Version", "1.1.0"), bearer);
    }
}
