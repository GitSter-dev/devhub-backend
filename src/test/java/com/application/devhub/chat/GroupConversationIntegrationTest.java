package com.application.devhub.chat;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GroupConversationIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    private ChatFixtures chat;
    private UUID ada;
    private UUID grace;
    private UUID linus;
    private String adaBearer;
    private String graceBearer;
    private String linusBearer;

    @BeforeEach
    void signIn() throws Exception {
        chat = new ChatFixtures(mockMvc, jsonMapper);
        ada = users.verified("ada");
        grace = users.verified("grace");
        linus = users.verified("linus");
        adaBearer = users.bearer("ada");
        graceBearer = users.bearer("grace");
        linusBearer = users.bearer("linus");
    }

    @Test
    void creatingAGroupMakesMeOwnerAndAnnouncesIt() throws Exception {
        UUID group = chat.group(adaBearer, "Rustaceans", List.of(grace, linus, grace));

        chat.fetch(graceBearer, "/conversations/{id}", group)
                .andExpect(jsonPath("$.data.kind").value("GROUP"))
                .andExpect(jsonPath("$.data.title").value("Rustaceans"))
                .andExpect(jsonPath("$.data.members", hasSize(3)))
                .andExpect(jsonPath("$.data.members[?(@.username == 'ada')].role").value(contains("OWNER")))
                .andExpect(jsonPath("$.data.unreadCount").value(0))
                .andExpect(jsonPath("$.data.lastMessage.system.type").value("GROUP_CREATED"))
                .andExpect(jsonPath("$.data.lastMessage.system.actor.username").value("ada"));
    }

    @Test
    void aGroupIsCappedAtFiftyMembers() throws Exception {
        List<UUID> crowd = new ArrayList<>();
        String hash = jdbcTemplate.queryForObject("SELECT password_hash FROM users WHERE username = 'ada'", String.class);
        for (int i = 0; i < 50; i++) {
            UUID id = UUID.randomUUID();
            jdbcTemplate.update("""
                    INSERT INTO users (id, username, display_name, email, password_hash, email_verified_at)
                    VALUES (?, ?, ?, ?, ?, now())
                    """, id, "crowd" + i, "Crowd " + i, "crowd" + i + "@dev.io", hash);
            crowd.add(id);
        }

        chat.createGroup(adaBearer, "Too big", crowd).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("GROUP_TOO_LARGE"));
        UUID group = chat.group(adaBearer, "Just right", crowd.subList(0, 49));
        chat.addMembers(adaBearer, group, grace).andExpect(jsonPath("$.error.code").value("GROUP_TOO_LARGE"));
    }

    @Test
    void onlyTheOwnerCanRenameAddOrRemove() throws Exception {
        UUID group = chat.group(adaBearer, "Old name", List.of(grace));

        chat.rename(graceBearer, group, "Hijacked").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_GROUP_OWNER"));
        chat.addMembers(graceBearer, group, linus).andExpect(status().isForbidden());

        chat.rename(adaBearer, group, "New name").andExpect(jsonPath("$.data.title").value("New name"))
                .andExpect(jsonPath("$.data.lastMessage.system.type").value("GROUP_RENAMED"));
        chat.addMembers(adaBearer, group, linus).andExpect(jsonPath("$.data.members", hasSize(3)));
        chat.fetch(linusBearer, "/conversations/{id}", group).andExpect(jsonPath("$.data.unreadCount").value(0))
                .andExpect(jsonPath("$.data.lastMessage.system.target.username").value("linus"));

        chat.removeMember(adaBearer, group, linus).andExpect(status().isOk());
        chat.fetch(linusBearer, "/conversations/{id}", group).andExpect(status().isNotFound());
        chat.fetch(adaBearer, "/conversations/{id}", group)
                .andExpect(jsonPath("$.data.lastMessage.system.type").value("MEMBER_REMOVED"));
    }

    @Test
    void whenTheOwnerLeavesTheLongestStandingMemberTakesOver() throws Exception {
        UUID group = chat.group(adaBearer, "Handover", List.of(grace, linus));

        chat.removeMember(adaBearer, group, ada).andExpect(status().isOk());

        chat.fetch(graceBearer, "/conversations/{id}", group)
                .andExpect(jsonPath("$.data.members", hasSize(2)))
                .andExpect(jsonPath("$.data.members[?(@.role == 'OWNER')].username").value(hasSize(1)))
                .andExpect(jsonPath("$.data.lastMessage.system.type").value("OWNER_CHANGED"));
        chat.fetch(adaBearer, "/conversations/{id}", group).andExpect(status().isNotFound());
    }

    @Test
    void directConversationsCannotBeLeftOrRenamed() throws Exception {
        UUID direct = chat.direct(adaBearer, grace);

        chat.removeMember(adaBearer, direct, ada).andExpect(status().isBadRequest());
        chat.rename(adaBearer, direct, "Nope").andExpect(status().isBadRequest());
    }
}
