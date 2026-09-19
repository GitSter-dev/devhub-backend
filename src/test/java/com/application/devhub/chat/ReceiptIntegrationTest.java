package com.application.devhub.chat;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReceiptIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    private ChatFixtures chat;
    private UUID grace;
    private UUID linus;
    private String adaBearer;
    private String graceBearer;
    private String linusBearer;

    @BeforeEach
    void signIn() throws Exception {
        chat = new ChatFixtures(mockMvc, jsonMapper);
        UUID ada = users.verified("ada");
        grace = users.verified("grace");
        linus = users.verified("linus");
        jdbcTemplate.update("INSERT INTO follows (follower_id, followee_id) VALUES (?, ?)", grace, ada);
        adaBearer = users.bearer("ada");
        graceBearer = users.bearer("grace");
        linusBearer = users.bearer("linus");
    }

    @Test
    void watermarksOnlyMoveForwardReadImpliesDeliveredAndTheyAreCapped() throws Exception {
        UUID conversation = chat.direct(adaBearer, grace);
        for (int i = 0; i < 5; i++) {
            chat.send(adaBearer, conversation, "m" + i);
        }

        chat.receipts(graceBearer, conversation, 0, 3).andExpect(jsonPath("$.data.deliveredSeq").value(3))
                .andExpect(jsonPath("$.data.readSeq").value(3));
        chat.receipts(graceBearer, conversation, 1, 1).andExpect(jsonPath("$.data.readSeq").value(3));
        chat.receipts(graceBearer, conversation, 999, 999).andExpect(jsonPath("$.data.deliveredSeq").value(5))
                .andExpect(jsonPath("$.data.readSeq").value(5));
    }

    @Test
    void theSenderSeesDeliveredThenReadAndTheRecipientsUnreadDrops() throws Exception {
        UUID conversation = chat.direct(adaBearer, grace);
        chat.send(adaBearer, conversation, "one");
        chat.send(adaBearer, conversation, "two");

        chat.fetch(graceBearer, "/conversations/{id}", conversation).andExpect(jsonPath("$.data.unreadCount").value(2));
        chat.fetch(adaBearer, "/conversations/{id}", conversation).andExpect(jsonPath("$.data.unreadCount").value(0))
                .andExpect(jsonPath("$.data.othersDeliveredSeq").value(0));

        chat.receipts(graceBearer, conversation, 2, 0);
        chat.fetch(adaBearer, "/conversations/{id}", conversation).andExpect(jsonPath("$.data.othersDeliveredSeq").value(2))
                .andExpect(jsonPath("$.data.othersReadSeq").value(0));

        chat.receipts(graceBearer, conversation, 2, 2);
        chat.fetch(adaBearer, "/conversations/{id}", conversation).andExpect(jsonPath("$.data.othersReadSeq").value(2));
        chat.fetch(graceBearer, "/conversations/{id}", conversation).andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    void inAGroupAMessageIsReadOnlyOnceEveryoneReadIt() throws Exception {
        UUID group = chat.group(adaBearer, "Trio", List.of(grace, linus));
        long seq = chat.sent(adaBearer, group, "hello both").get("seq").asLong();

        chat.receipts(graceBearer, group, seq, seq);
        chat.fetch(adaBearer, "/conversations/{id}", group).andExpect(jsonPath("$.data.othersReadSeq").value(seq - 1));

        chat.receipts(linusBearer, group, seq, seq);
        chat.fetch(adaBearer, "/conversations/{id}", group).andExpect(jsonPath("$.data.othersReadSeq").value(seq));
    }

    @Test
    void outsidersCannotReportReceipts() throws Exception {
        UUID conversation = chat.direct(adaBearer, grace);

        chat.receipts(linusBearer, conversation, 1, 1).andExpect(status().isNotFound());
    }
}
