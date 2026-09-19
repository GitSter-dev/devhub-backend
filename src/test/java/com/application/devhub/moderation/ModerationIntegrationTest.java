package com.application.devhub.moderation;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ModerationIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestUsers users;

    private ModerationFixtures fixtures;
    private UUID adaId;
    private String ada;
    private String ken;
    private String linus;
    private String grace;
    private String admin;
    private String mira;
    private UUID post;

    @BeforeEach
    void setUp() throws Exception {
        fixtures = new ModerationFixtures(mockMvc, jsonMapper, jdbcTemplate);
        adaId = users.verified("ada", "Ada");
        users.verified("ken", "Ken");
        users.verified("linus", "Linus");
        users.verified("grace", "Grace");
        users.admin("root");
        users.verified("mira", "Mira");
        ada = users.bearer("ada");
        ken = users.bearer("ken");
        linus = users.bearer("linus");
        grace = users.bearer("grace");
        admin = users.bearer("root");
        mira = users.bearer("mira");
        post = fixtures.publish(ada, "Something reportable");
    }

    @Test
    void aReportKeepsTheEvidenceEvenAfterTheAuthorDeletesThePost() throws Exception {
        fixtures.report(ken, "POST", post, "HARASSMENT").andExpect(status().isOk());

        fixtures.perform(ada, delete("/posts/{id}", post)).andExpect(status().isOk());

        JsonNode detail = fixtures.caseDetail(admin, fixtures.onlyCaseId());
        assertThat(detail.get("reports")).hasSize(1);
        JsonNode report = detail.get("reports").get(0);
        assertThat(report.get("reporterUsername").asString()).isEqualTo("ken");
        assertThat(report.get("reason").asString()).isEqualTo("HARASSMENT");
        assertThat(report.get("snapshot").asString()).contains("Something reportable").contains("ada");
    }

    @Test
    void whatYouReportDisappearsForYouStraightAway() throws Exception {
        assertThat(visibleTo(ken, post)).isTrue();

        fixtures.report(ken, "POST", post, "SPAM").andExpect(status().isOk());

        assertThat(visibleTo(ken, post)).isFalse();
        assertThat(visibleTo(linus, post)).isTrue();
    }

    @Test
    void reportsOnTheSameItemShareOneCaseAndEachPersonCountsOnce() throws Exception {
        fixtures.report(ken, "POST", post, "SPAM");
        fixtures.report(ken, "POST", post, "HATE");
        fixtures.report(linus, "POST", post, "HATE");

        JsonNode cases = fixtures.cases(admin, "OPEN");
        assertThat(cases).hasSize(1);
        assertThat(cases.get(0).get("reporterCount").asInt()).isEqualTo(2);
        assertThat(cases.get(0).get("severity").asInt()).isEqualTo(3);
        assertThat(cases.get(0).get("ownerUsername").asString()).isEqualTo("ada");
    }

    @Test
    void youCannotReportYourOwnContentOrSomethingYouCannotSee() throws Exception {
        fixtures.report(ada, "POST", post, "SPAM").andExpect(status().isBadRequest());
        fixtures.report(ken, "POST", UUID.randomUUID(), "SPAM").andExpect(status().isNotFound());

        fixtures.perform(ada, put("/users/me/blocks/{id}", idOf("ken"))).andExpect(status().isOk());

        fixtures.report(ken, "POST", post, "SPAM").andExpect(status().isNotFound());
    }

    @Test
    void threeEstablishedReportersHideThePostUntilAModeratorLooks() throws Exception {
        List.of("ken", "linus", "grace").forEach(username -> fixtures.ageAccount(idOf(username), 30));

        fixtures.report(ken, "POST", post, "SPAM");
        fixtures.report(linus, "POST", post, "SPAM");
        assertThat(visibleTo(mira, post)).isTrue();

        fixtures.report(grace, "POST", post, "SPAM");

        assertThat(visibleTo(mira, post)).isFalse();
        fixtures.perform(ada, get("/posts/{id}", post))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.post.underReview").value(true));
        assertThat(fixtures.cases(admin, "OPEN").get(0).get("autoHidden").asBoolean()).isTrue();
    }

    @Test
    void brandNewAccountsCannotHideAPostByReportingIt() throws Exception {
        fixtures.report(ken, "POST", post, "SPAM");
        fixtures.report(linus, "POST", post, "SPAM");
        fixtures.report(grace, "POST", post, "SPAM");

        assertThat(visibleTo(mira, post)).isTrue();
        assertThat(fixtures.cases(admin, "OPEN").get(0).get("autoHidden").asBoolean()).isFalse();
    }

    @Test
    void removingAPostHidesItFromEveryoneElseAndTellsTheReporters() throws Exception {
        fixtures.report(ken, "POST", post, "HATE");

        fixtures.act(admin, fixtures.onlyCaseId(), "REMOVE_CONTENT", null).andExpect(status().isOk());

        assertThat(visibleTo(mira, post)).isFalse();
        fixtures.perform(ada, get("/posts/{id}", post))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.post.removed").value(true));
        JsonNode notifications = fixtures.data(fixtures.perform(ken, get("/notifications"))).get("items");
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).get("type").asString()).isEqualTo("REPORT_RESOLVED");
        assertThat(fixtures.cases(admin, "ACTIONED")).hasSize(1);
    }

    @Test
    void dismissingPutsAnAutoHiddenPostBack() throws Exception {
        List.of("ken", "linus", "grace").forEach(username -> fixtures.ageAccount(idOf(username), 30));
        fixtures.report(ken, "POST", post, "SPAM");
        fixtures.report(linus, "POST", post, "SPAM");
        fixtures.report(grace, "POST", post, "SPAM");

        fixtures.act(admin, fixtures.onlyCaseId(), "DISMISS", null).andExpect(status().isOk());

        assertThat(visibleTo(mira, post)).isTrue();
        assertThat(fixtures.cases(admin, "DISMISSED")).hasSize(1);
    }

    @Test
    void suspendingSignsThemOutAndExplainsWhyAtTheNextLogin() throws Exception {
        fixtures.report(ken, "POST", post, "HARASSMENT");

        fixtures.act(admin, fixtures.onlyCaseId(), "SUSPEND", 7).andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM refresh_tokens WHERE user_id = ? AND revoked_at IS NOT NULL", Long.class, adaId))
                .isPositive();
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"ada\",\"password\":\"" + TestUsers.PASSWORD + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_SUSPENDED"));
    }

    @Test
    void banningHidesTheirProfileAndPosts() throws Exception {
        fixtures.report(ken, "POST", post, "HATE");

        fixtures.act(admin, fixtures.onlyCaseId(), "BAN", null).andExpect(status().isOk());

        fixtures.perform(ken, get("/users/ada/profile")).andExpect(status().isNotFound());
        assertThat(visibleTo(mira, post)).isFalse();
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"ada\",\"password\":\"" + TestUsers.PASSWORD + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_BANNED"));
    }

    @Test
    void reinstatingLetsThemSignInAgain() throws Exception {
        fixtures.report(ken, "POST", post, "HATE");
        UUID caseId = fixtures.onlyCaseId();
        fixtures.act(admin, caseId, "SUSPEND", 7).andExpect(status().isOk());

        fixtures.act(admin, caseId, "REINSTATE", null).andExpect(status().isOk());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"ada\",\"password\":\"" + TestUsers.PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void reportingAMessageStoresTheConversationAroundIt() throws Exception {
        fixtures.perform(ada, put("/users/me/following/{id}", idOf("ken"))).andExpect(status().isOk());
        UUID conversation = UUID.fromString(fixtures.data(fixtures.perform(ken, post("/conversations/direct")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\": \"" + adaId + "\"}")))
                .get("id").asString());
        send(ken, conversation, "first");
        send(ken, conversation, "second");
        UUID reported = send(ken, conversation, "the bad one");

        fixtures.report(ada, "MESSAGE", reported, "HARASSMENT").andExpect(status().isOk());

        String snapshot = fixtures.caseDetail(admin, fixtures.onlyCaseId()).get("reports").get(0).get("snapshot").asString();
        assertThat(snapshot).contains("the bad one").contains("first").contains("second");
    }

    @Test
    void theQueueIsClosedToEveryoneButAdministrators() throws Exception {
        fixtures.report(ken, "POST", post, "SPAM");

        fixtures.perform(ken, get("/admin/moderation/cases")).andExpect(status().isForbidden());
        fixtures.perform(ken, post("/admin/moderation/cases/{id}/actions", fixtures.onlyCaseId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"action\": \"DISMISS\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/moderation/cases")).andExpect(status().isUnauthorized());
    }

    private UUID send(String bearer, UUID conversationId, String body) throws Exception {
        return UUID.fromString(fixtures.data(fixtures.perform(bearer,
                        post("/conversations/{id}/messages", conversationId).contentType(MediaType.APPLICATION_JSON)
                                .content("{\"clientMessageId\": \"" + UUID.randomUUID() + "\", \"body\": \"" + body + "\"}"))
                .andExpect(status().isCreated())).get("id").asString());
    }

    private boolean visibleTo(String bearer, UUID postId) throws Exception {
        return fixtures.perform(bearer, get("/posts/{id}", postId)).andReturn().getResponse().getStatus() == 200;
    }

    private UUID idOf(String username) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", UUID.class, username);
    }
}
