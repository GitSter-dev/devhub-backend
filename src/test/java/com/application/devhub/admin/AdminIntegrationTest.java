package com.application.devhub.admin;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestUsers users;

    private UUID adaId;
    private String admin;
    private String ada;

    @BeforeEach
    void setUp() throws Exception {
        adaId = users.verified("ada", "Ada Lovelace");
        users.admin("root");
        admin = users.bearer("root");
        ada = users.bearer("ada");
    }

    @Test
    void everyAdminRouteIsClosedToEveryoneButAdministrators() throws Exception {
        for (String path : List.of("/admin/users?q=ada", "/admin/users/" + adaId, "/admin/stats",
                "/admin/moderation/actions")) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
            perform(ada, get(path)).andExpect(status().isForbidden());
            perform(admin, get(path)).andExpect(status().isOk());
        }
    }

    @Test
    void searchFindsAccountsPeopleSearchHides() throws Exception {
        UUID banned = users.verified("adabanned", "Banned");
        jdbcTemplate.update("UPDATE users SET banned_at = now() WHERE id = ?", banned);
        users.unverified("adanew");
        UUID suspended = users.verified("adasusp", "Suspended");
        jdbcTemplate.update("UPDATE users SET suspended_until = now() + interval '1 day' WHERE id = ?", suspended);

        JsonNode found = data(perform(admin, get("/admin/users").param("q", "ada")).andExpect(status().isOk()));

        assertThat(found).extracting(user -> user.get("username").asString())
                .contains("ada", "adabanned", "adanew", "adasusp");
        assertThat(statusOf(found, "ada")).isEqualTo("ACTIVE");
        assertThat(statusOf(found, "adabanned")).isEqualTo("BANNED");
        assertThat(statusOf(found, "adanew")).isEqualTo("UNVERIFIED");
        assertThat(statusOf(found, "adasusp")).isEqualTo("SUSPENDED");
    }

    @Test
    void searchMatchesAnExactEmailAndTypos() throws Exception {
        users.verified("linus", "Linus Torvalds");

        JsonNode byEmail = data(perform(admin, get("/admin/users").param("q", "LINUS@dev.io")));
        assertThat(byEmail.get(0).get("username").asString()).isEqualTo("linus");
        assertThat(byEmail.get(0).get("email").asString()).isEqualTo("linus@dev.io");

        JsonNode byTypo = data(perform(admin, get("/admin/users").param("q", "torvlds")));
        assertThat(byTypo).extracting(user -> user.get("username").asString()).contains("linus");

        assertThat(data(perform(admin, get("/admin/users").param("q", "a")))).isEmpty();
    }

    @Test
    void anAccountShowsItsActivityAndTheCasesAboutIt() throws Exception {
        users.verified("ken", "Ken");
        String ken = users.bearer("ken");
        UUID post = publish(ada, "First");
        publish(ada, "Second");
        report(ken, post);

        JsonNode detail = data(perform(admin, get("/admin/users/{id}", adaId)).andExpect(status().isOk()));

        assertThat(detail.get("user").get("username").asString()).isEqualTo("ada");
        assertThat(detail.get("user").get("role").asString()).isEqualTo("USER");
        assertThat(detail.get("emailVerifiedAt").isNull()).isFalse();
        assertThat(detail.get("postCount").asLong()).isEqualTo(2);
        assertThat(detail.get("reportsFiled").asLong()).isZero();
        assertThat(detail.get("reportsAgainst").asLong()).isEqualTo(1);
        assertThat(detail.get("cases")).hasSize(1);
        assertThat(detail.get("cases").get(0).get("targetId").asString()).isEqualTo(post.toString());

        perform(admin, get("/admin/users/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void statsCountTotalsAndBucketTodaysActivity() throws Exception {
        users.verified("ken", "Ken");
        users.unverified("newbie");
        UUID banned = users.verified("mallory", "Mallory");
        jdbcTemplate.update("UPDATE users SET banned_at = now() WHERE id = ?", banned);
        UUID post = publish(ada, "Hello");
        report(users.bearer("ken"), post);

        JsonNode overview = data(perform(admin, get("/admin/stats").param("days", "7")).andExpect(status().isOk()));

        JsonNode totals = overview.get("totals");
        assertThat(totals.get("users").asLong()).isEqualTo(5);
        assertThat(totals.get("verifiedUsers").asLong()).isEqualTo(4);
        assertThat(totals.get("posts").asLong()).isEqualTo(1);
        assertThat(totals.get("openCases").asLong()).isEqualTo(1);
        assertThat(totals.get("banned").asLong()).isEqualTo(1);
        assertThat(totals.get("suspended").asLong()).isZero();

        JsonNode daily = overview.get("daily");
        assertThat(daily).hasSize(7);
        JsonNode today = daily.get(6);
        assertThat(today.get("date").asString()).isEqualTo(LocalDate.now(ZoneOffset.UTC).toString());
        assertThat(today.get("signups").asLong()).isEqualTo(5);
        assertThat(today.get("posts").asLong()).isEqualTo(1);
        assertThat(today.get("reports").asLong()).isEqualTo(1);
        assertThat(daily.get(0).get("signups").asLong()).isZero();
    }

    @Test
    void statsHistoryIsClampedToNinetyDays() throws Exception {
        assertThat(data(perform(admin, get("/admin/stats").param("days", "1000"))).get("daily")).hasSize(90);
        assertThat(data(perform(admin, get("/admin/stats").param("days", "0"))).get("daily")).hasSize(1);
    }

    private UUID publish(String bearer, String body) throws Exception {
        return UUID.fromString(data(perform(bearer, post("/posts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\": \"" + body + "\"}")).andExpect(status().isCreated())).get("id").asString());
    }

    private void report(String bearer, UUID post) throws Exception {
        perform(bearer, post("/reports").contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetType\": \"POST\", \"targetId\": \"%s\", \"reason\": \"SPAM\"}".formatted(post)))
                .andExpect(status().isOk());
    }

    private static String statusOf(JsonNode users, String username) {
        for (JsonNode user : users) {
            if (user.get("username").asString().equals(username)) {
                return user.get("status").asString();
            }
        }
        throw new AssertionError(username + " not found");
    }

    private ResultActions perform(String bearer, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private JsonNode data(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("data");
    }
}
