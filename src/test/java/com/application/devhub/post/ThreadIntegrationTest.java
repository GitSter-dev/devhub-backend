package com.application.devhub.post;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ThreadIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    private PostFixtures posts;
    private String ada;
    private String grace;

    @BeforeEach
    void signIn() throws Exception {
        posts = new PostFixtures(mockMvc, jdbcTemplate, jsonMapper);
        users.verified("ada");
        users.verified("grace");
        ada = users.bearer("ada");
        grace = users.bearer("grace");
    }

    @Test
    void aDeepReplyComesWithEveryAncestorOldestFirst() throws Exception {
        UUID root = posts.publish(ada, "root");
        UUID first = posts.reply(grace, root, "first");
        UUID second = posts.reply(ada, first, "second");
        UUID third = posts.reply(grace, second, "third");

        mockMvc.perform(get("/posts/{id}", third).header(HttpHeaders.AUTHORIZATION, ada))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.post.body").value("third"))
                .andExpect(jsonPath("$.data.ancestors[*].body", contains("root", "first", "second")));
        mockMvc.perform(get("/posts/{id}", root).header(HttpHeaders.AUTHORIZATION, ada))
                .andExpect(jsonPath("$.data.ancestors", empty()))
                .andExpect(jsonPath("$.data.post.replyCount").value(1));
    }

    @Test
    void repliesAreListedOldestFirstInPages() throws Exception {
        UUID root = posts.publish(ada, "root");
        for (int i = 0; i < 25; i++) {
            jdbcTemplate.update("""
                    INSERT INTO posts (id, author_id, body, parent_id, root_id, created_at)
                    SELECT ?, id, ?, ?, ?, now() + make_interval(secs => ?) FROM users WHERE username = 'grace'
                    """, UUID.randomUUID(), "reply-" + i, root, root, i);
        }

        ResultActions first = replies(root, null)
                .andExpect(jsonPath("$.data.items", hasSize(20)))
                .andExpect(jsonPath("$.data.items[0].body").value("reply-0"));
        replies(root, posts.nextCursor(first))
                .andExpect(jsonPath("$.data.items[*].body", contains("reply-20", "reply-21", "reply-22", "reply-23", "reply-24")))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
    }

    @Test
    void aDeletedReplyIsOnlyListedWhileItHasRepliesOfItsOwn() throws Exception {
        UUID root = posts.publish(ada, "root");
        UUID lonely = posts.reply(grace, root, "lonely");
        UUID busy = posts.reply(grace, root, "busy");
        posts.reply(ada, busy, "answer");
        jdbcTemplate.update("UPDATE posts SET deleted_at = now(), body = NULL WHERE id IN (?, ?)", lonely, busy);

        replies(root, null)
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].id").value(busy.toString()))
                .andExpect(jsonPath("$.data.items[0].deleted").value(true));
    }

    @Test
    void aDevelopersPostsAreTheirOriginalsNewestFirst() throws Exception {
        UUID older = posts.publish(grace, "older");
        posts.publish(grace, "newer");
        posts.reply(grace, older, "a reply");

        mockMvc.perform(get("/users/grace/posts").header(HttpHeaders.AUTHORIZATION, ada))
                .andExpect(jsonPath("$.data.items[*].body", contains("newer", "older")));
    }

    @Test
    void unknownPostsAreNotFound() throws Exception {
        mockMvc.perform(get("/posts/{id}", UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, ada))
                .andExpect(status().isNotFound());
        replies(UUID.randomUUID(), null).andExpect(status().isNotFound());
    }

    private ResultActions replies(UUID post, String cursor) throws Exception {
        var request = get("/posts/{id}/replies", post).header(HttpHeaders.AUTHORIZATION, ada);
        return mockMvc.perform(cursor == null ? request : request.param("cursor", cursor));
    }
}
