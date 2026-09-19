package com.application.devhub.chat;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MessageIntegrationTest extends IntegrationTest {

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
    private String adaBearer;
    private String graceBearer;
    private String linusBearer;
    private UUID conversation;

    @BeforeEach
    void signIn() throws Exception {
        chat = new ChatFixtures(mockMvc, jsonMapper);
        UUID ada = users.verified("ada");
        grace = users.verified("grace");
        users.verified("linus");
        jdbcTemplate.update("INSERT INTO follows (follower_id, followee_id) VALUES (?, ?)", grace, ada);
        adaBearer = users.bearer("ada");
        graceBearer = users.bearer("grace");
        linusBearer = users.bearer("linus");
        conversation = chat.direct(adaBearer, grace);
    }

    @Test
    void sendingStoresTheMessageWithTheNextSeq() throws Exception {
        chat.send(adaBearer, conversation, "first").andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.seq").value(1))
                .andExpect(jsonPath("$.data.sender.username").value("ada"))
                .andExpect(jsonPath("$.data.kind").value("TEXT"));
        chat.sendJson(adaBearer, conversation, """
                {"clientMessageId": "%s", "code": "fn main() {}", "codeLanguage": "Rust"}
                """.formatted(UUID.randomUUID()))
                .andExpect(jsonPath("$.data.seq").value(2))
                .andExpect(jsonPath("$.data.codeLanguage").value("rust"));
    }

    @Test
    void concurrentSendsGetDistinctGaplessSeqs() throws Exception {
        int sends = 20;
        List<Future<Long>> futures = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            for (int i = 0; i < sends; i++) {
                String bearer = i % 2 == 0 ? adaBearer : graceBearer;
                futures.add(pool.submit(() -> chat.sent(bearer, conversation, "parallel").get("seq").asLong()));
            }
        }
        List<Long> seqs = new ArrayList<>();
        for (Future<Long> future : futures) {
            seqs.add(future.get());
        }

        assertThat(seqs).containsExactlyInAnyOrderElementsOf(LongStream.rangeClosed(1, sends).boxed().toList());
    }

    @Test
    void resendingTheSameClientMessageIdNeverDuplicates() throws Exception {
        UUID clientId = UUID.randomUUID();
        String json = "{\"clientMessageId\": \"" + clientId + "\", \"body\": \"once\"}";

        JsonNode first = chat.data(chat.sendJson(adaBearer, conversation, json));
        JsonNode retry = chat.data(chat.sendJson(adaBearer, conversation, json).andExpect(status().isCreated()));

        assertThat(retry.get("id").asString()).isEqualTo(first.get("id").asString());
        assertThat(retry.get("seq").asLong()).isEqualTo(1);
        UUID other = chat.direct(adaBearer, users.verified("ken"));
        chat.sendJson(adaBearer, other, json).andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    void repliesQuoteTheOriginalAndMustStayInTheConversation() throws Exception {
        String original = chat.sent(adaBearer, conversation, "the original").get("id").asString();

        chat.sendJson(graceBearer, conversation, """
                {"clientMessageId": "%s", "body": "a reply", "replyToId": "%s"}
                """.formatted(UUID.randomUUID(), original))
                .andExpect(jsonPath("$.data.replyTo.id").value(original))
                .andExpect(jsonPath("$.data.replyTo.senderName").value("ada"))
                .andExpect(jsonPath("$.data.replyTo.preview").value("the original"));

        UUID elsewhere = chat.direct(graceBearer, users.verified("ken"));
        chat.sendJson(graceBearer, elsewhere, """
                {"clientMessageId": "%s", "body": "sneaky", "replyToId": "%s"}
                """.formatted(UUID.randomUUID(), original)).andExpect(status().isNotFound());
    }

    @Test
    void outsidersCanNeitherReadNorWrite() throws Exception {
        chat.send(adaBearer, conversation, "private");

        chat.fetch(linusBearer, "/conversations/{id}/messages", conversation).andExpect(status().isNotFound());
        chat.send(linusBearer, conversation, "let me in").andExpect(status().isNotFound());
    }

    @Test
    void onlyTheSenderCanDeleteAndTheMessageStaysAsAPlaceholder() throws Exception {
        UUID message = UUID.fromString(chat.sent(adaBearer, conversation, "oops").get("id").asString());
        chat.sendJson(graceBearer, conversation, """
                {"clientMessageId": "%s", "body": "what?", "replyToId": "%s"}
                """.formatted(UUID.randomUUID(), message));

        chat.deleteMessage(graceBearer, conversation, message).andExpect(status().isForbidden());
        chat.deleteMessage(adaBearer, conversation, message).andExpect(status().isOk());

        chat.fetch(adaBearer, "/conversations/{id}/messages", conversation)
                .andExpect(jsonPath("$.data.items[0].deleted").value(true))
                .andExpect(jsonPath("$.data.items[0].body").doesNotExist())
                .andExpect(jsonPath("$.data.items[1].replyTo.deleted").value(true))
                .andExpect(jsonPath("$.data.items[1].replyTo.preview").doesNotExist());
    }

    @Test
    void historyPagesBackwardAndGapsFillForward() throws Exception {
        for (int i = 1; i <= 35; i++) {
            chat.send(i % 2 == 0 ? adaBearer : graceBearer, conversation, "m" + i);
        }

        chat.fetch(adaBearer, "/conversations/{id}/messages", conversation)
                .andExpect(jsonPath("$.data.items", hasSize(30)))
                .andExpect(jsonPath("$.data.items[0].seq").value(6))
                .andExpect(jsonPath("$.data.items[29].seq").value(35))
                .andExpect(jsonPath("$.data.hasMore").value(true));
        mockMvc.perform(get("/conversations/{id}/messages", conversation).param("beforeSeq", "6")
                        .header(HttpHeaders.AUTHORIZATION, adaBearer))
                .andExpect(jsonPath("$.data.items[*].seq", contains(1, 2, 3, 4, 5)))
                .andExpect(jsonPath("$.data.hasMore").value(false));
        mockMvc.perform(get("/conversations/{id}/messages", conversation).param("afterSeq", "32")
                        .header(HttpHeaders.AUTHORIZATION, adaBearer))
                .andExpect(jsonPath("$.data.items[*].seq", contains(33, 34, 35)))
                .andExpect(jsonPath("$.data.hasMore").value(false));
    }
}
