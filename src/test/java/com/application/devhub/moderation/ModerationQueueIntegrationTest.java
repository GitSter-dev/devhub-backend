package com.application.devhub.moderation;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ModerationQueueIntegrationTest extends IntegrationTest {

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
    private UUID rootId;
    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        fixtures = new ModerationFixtures(mockMvc, jsonMapper, jdbcTemplate);
        adaId = users.verified("ada", "Ada");
        rootId = users.admin("root");
        admin = users.bearer("root");
    }

    @Test
    void theQueuePagesInSeverityOrderWithoutSkippingOrRepeatingCases() throws Exception {
        for (int i = 0; i < 45; i++) {
            jdbcTemplate.update("""
                    INSERT INTO moderation_cases (id, target_type, target_id, owner_id, reporter_count, severity,
                                                  last_reported_at)
                    VALUES (gen_random_uuid(), 'POST', gen_random_uuid(), ?, ?, ?,
                            now() - make_interval(hours => ?))
                    """, adaId, 1 + i % 3, 1 + i % 5, i % 4);
        }
        List<UUID> expected = jdbcTemplate.queryForList("""
                SELECT id FROM moderation_cases
                ORDER BY severity DESC, reporter_count DESC, last_reported_at DESC, id DESC
                """, UUID.class);

        List<UUID> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do {
            JsonNode page = fixtures.data(fixtures.perform(admin, get("/admin/moderation/cases")
                    .param("status", "OPEN").param("cursor", cursor)).andExpect(status().isOk()));
            page.get("items").forEach(item -> seen.add(UUID.fromString(item.get("id").asString())));
            cursor = page.get("nextCursor").isNull() ? null : page.get("nextCursor").asString();
            pages++;
        } while (cursor != null);

        assertThat(pages).isEqualTo(3);
        assertThat(seen).containsExactlyElementsOf(expected);
    }

    @Test
    void aTamperedQueueCursorIsABadRequest() throws Exception {
        fixtures.perform(admin, get("/admin/moderation/cases").param("cursor", "not-a-cursor"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theAuditLogListsActionsNewestFirstAndFiltersByPerson() throws Exception {
        String ada = users.bearer("ada");
        users.verified("ken", "Ken");
        users.verified("linus", "Linus");
        UUID post = fixtures.publish(ada, "Something reportable");
        fixtures.report(users.bearer("ken"), "POST", post, "HARASSMENT").andExpect(status().isOk());
        UUID caseId = fixtures.onlyCaseId();
        fixtures.act(admin, caseId, "SUSPEND", 7).andExpect(status().isOk());
        fixtures.act(admin, caseId, "REINSTATE", null).andExpect(status().isOk());
        fixtures.report(ada, "USER", users.verified("mallory", "Mallory"), "SPAM").andExpect(status().isOk());
        UUID otherCase = jdbcTemplate.queryForObject(
                "SELECT id FROM moderation_cases WHERE target_type = 'USER'", UUID.class);
        fixtures.act(admin, otherCase, "BAN", null).andExpect(status().isOk());

        JsonNode all = fixtures.data(fixtures.perform(admin, get("/admin/moderation/actions"))
                .andExpect(status().isOk()));
        assertThat(all.get("items")).extracting(entry -> entry.get("action").asString())
                .containsExactly("BAN", "REINSTATE", "SUSPEND");
        JsonNode suspend = all.get("items").get(2);
        assertThat(suspend.get("moderatorUsername").asString()).isEqualTo("root");
        assertThat(suspend.get("targetUsername").asString()).isEqualTo("ada");
        assertThat(suspend.get("caseId").asString()).isEqualTo(caseId.toString());
        assertThat(suspend.get("actsUntil").isNull()).isFalse();

        JsonNode adas = fixtures.data(fixtures.perform(admin, get("/admin/moderation/actions")
                .param("userId", adaId.toString())).andExpect(status().isOk()));
        assertThat(adas.get("items")).extracting(entry -> entry.get("action").asString())
                .containsExactly("REINSTATE", "SUSPEND");
    }

    @Test
    void theAuditLogPagesThroughEveryEntryOnce() throws Exception {
        for (int i = 0; i < 60; i++) {
            jdbcTemplate.update("""
                    INSERT INTO moderation_actions (id, moderator_id, action, target_user_id, created_at)
                    VALUES (gen_random_uuid(), ?, 'WARN', ?, now() - make_interval(mins => ?))
                    """, rootId, adaId, i % 7);
        }

        JsonNode first = fixtures.data(fixtures.perform(admin, get("/admin/moderation/actions"))
                .andExpect(status().isOk()));
        JsonNode second = fixtures.data(fixtures.perform(admin, get("/admin/moderation/actions")
                .param("cursor", first.get("nextCursor").asString())).andExpect(status().isOk()));

        List<String> ids = new ArrayList<>();
        first.get("items").forEach(item -> ids.add(item.get("id").asString()));
        second.get("items").forEach(item -> ids.add(item.get("id").asString()));
        assertThat(first.get("items")).hasSize(50);
        assertThat(second.get("nextCursor").isNull()).isTrue();
        assertThat(ids).hasSize(60).doesNotHaveDuplicates();
    }
}
