package com.application.devhub.chat;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DirectConversationIntegrationTest extends IntegrationTest {

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
    private String adaBearer;
    private String graceBearer;

    @BeforeEach
    void signIn() throws Exception {
        chat = new ChatFixtures(mockMvc, jsonMapper);
        ada = users.verified("ada");
        grace = users.verified("grace");
        adaBearer = users.bearer("ada");
        graceBearer = users.bearer("grace");
    }

    @Test
    void openingADirectConversationIsStable() throws Exception {
        String first = chat.data(chat.openDirect(adaBearer, grace).andExpect(status().isCreated())).get("id").asString();

        String again = chat.data(chat.openDirect(adaBearer, grace).andExpect(status().isOk())).get("id").asString();
        String fromGrace = chat.data(chat.openDirect(graceBearer, ada).andExpect(status().isOk())).get("id").asString();

        assertThat(again).isEqualTo(first);
        assertThat(fromGrace).isEqualTo(first);
    }

    @Test
    void youCannotMessageYourselfOrUnknownPeople() throws Exception {
        chat.openDirect(adaBearer, ada).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CANNOT_MESSAGE_YOURSELF"));
        chat.openDirect(adaBearer, UUID.randomUUID()).andExpect(status().isNotFound());
    }

    @Test
    void aStrangersMessageLandsInRequestsAndCannotBeAnsweredUntilAccepted() throws Exception {
        UUID conversation = chat.direct(adaBearer, grace);
        chat.send(adaBearer, conversation, "hi grace").andExpect(status().isCreated());

        chat.fetch(graceBearer, "/conversations").andExpect(jsonPath("$.data.items", empty()));
        chat.fetch(graceBearer, "/conversations/requests")
                .andExpect(jsonPath("$.data[*].id", contains(conversation.toString())))
                .andExpect(jsonPath("$.data[0].myStatus").value("REQUEST"));
        chat.send(graceBearer, conversation, "hello").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CONVERSATION_REQUEST_PENDING"));

        chat.postTo(graceBearer, "/conversations/{id}/accept", conversation).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myStatus").value("ACTIVE"));
        chat.send(graceBearer, conversation, "hello").andExpect(status().isCreated());
        chat.fetch(graceBearer, "/conversations").andExpect(jsonPath("$.data.items[*].id", contains(conversation.toString())));
    }

    @Test
    void someoneWhoFollowsYouGetsYourMessageStraightInTheirInbox() throws Exception {
        jdbcTemplate.update("INSERT INTO follows (follower_id, followee_id) VALUES (?, ?)", grace, ada);
        UUID conversation = chat.direct(adaBearer, grace);
        chat.send(adaBearer, conversation, "hi");

        chat.fetch(graceBearer, "/conversations").andExpect(jsonPath("$.data.items[0].myStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.items[0].unreadCount").value(1));
        chat.fetch(graceBearer, "/conversations/requests").andExpect(jsonPath("$.data", empty()));
    }

    @Test
    void readReceiptsStayHiddenFromTheSenderUntilTheRequestIsAccepted() throws Exception {
        UUID conversation = chat.direct(adaBearer, grace);
        chat.send(adaBearer, conversation, "hi");
        chat.receipts(graceBearer, conversation, 1, 1).andExpect(status().isOk());

        chat.fetch(adaBearer, "/conversations/{id}", conversation)
                .andExpect(jsonPath("$.data.othersReadSeq").value(0))
                .andExpect(jsonPath("$.data.members[?(@.username == 'grace')].readSeq").value(contains((Object) null)));

        chat.postTo(graceBearer, "/conversations/{id}/accept", conversation);
        chat.fetch(adaBearer, "/conversations/{id}", conversation).andExpect(jsonPath("$.data.othersReadSeq").value(1));
    }

    @Test
    void aDeclinedRequestDisappearsUntilTheRecipientReachesOutThemselves() throws Exception {
        UUID conversation = chat.direct(adaBearer, grace);
        chat.send(adaBearer, conversation, "hi");

        chat.postTo(graceBearer, "/conversations/{id}/decline", conversation).andExpect(status().isOk());
        chat.fetch(graceBearer, "/conversations/requests").andExpect(jsonPath("$.data", empty()));
        chat.fetch(graceBearer, "/conversations/{id}", conversation).andExpect(status().isNotFound());

        chat.openDirect(graceBearer, ada).andExpect(jsonPath("$.data.myStatus").value("ACTIVE"));
    }
}
