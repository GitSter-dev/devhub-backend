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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FeedIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    private PostFixtures posts;
    private UUID me;
    private UUID followed;
    private UUID kindred;
    private UUID stranger;
    private String bearer;

    @BeforeEach
    void seed() throws Exception {
        posts = new PostFixtures(mockMvc, jdbcTemplate, jsonMapper);
        me = users.verified("me");
        followed = users.verified("followed");
        kindred = users.verified("kindred");
        stranger = users.verified("stranger");
        posts.topics(me, "rust", "go");
        posts.topics(kindred, "rust");
        posts.topics(stranger, "design");
        posts.follow(me, followed);
        bearer = users.bearer("me");
    }

    @Test
    void followedAndOwnPostsComeFirstThenPostsFromPeopleWhoShareMyTopics() throws Exception {
        posts.insert(followed, "followed-new", 1);
        posts.insert(me, "mine", 5);
        posts.insert(followed, "followed-old", 10);
        posts.insert(kindred, "kindred-newest-of-all", 0);
        posts.insert(stranger, "stranger", 0);

        feed(null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].body",
                        contains("followed-new", "mine", "followed-old", "kindred-newest-of-all")))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
    }

    @Test
    void repliesAndDeletedPostsNeverAppear() throws Exception {
        UUID original = posts.insert(followed, "original", 3);
        UUID gone = posts.insert(followed, "gone", 2);
        jdbcTemplate.update("INSERT INTO posts (id, author_id, body, parent_id, root_id) VALUES (?, ?, 'a reply', ?, ?)",
                UUID.randomUUID(), followed, original, original);
        jdbcTemplate.update("UPDATE posts SET deleted_at = now(), body = NULL WHERE id = ?", gone);

        feed(null).andExpect(jsonPath("$.data.items[*].body", contains("original")));
    }

    @Test
    void aPageThatRunsOutOfFollowedPostsContinuesWithInterests() throws Exception {
        for (int i = 0; i < 18; i++) {
            posts.insert(followed, "followed-" + i, 100 + i);
        }
        for (int i = 0; i < 10; i++) {
            posts.insert(kindred, "kindred-" + i, 10 + i);
        }

        ResultActions first = feed(null)
                .andExpect(jsonPath("$.data.items", hasSize(20)))
                .andExpect(jsonPath("$.data.items[17].body").value("followed-17"))
                .andExpect(jsonPath("$.data.items[18].body").value("kindred-0"))
                .andExpect(jsonPath("$.data.items[19].body").value("kindred-1"));

        feed(posts.nextCursor(first))
                .andExpect(jsonPath("$.data.items", hasSize(8)))
                .andExpect(jsonPath("$.data.items[0].body").value("kindred-2"))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
    }

    @Test
    void aFullPageOfFollowedPostsKeepsTheCursorInTheFollowingTier() throws Exception {
        for (int i = 0; i < 25; i++) {
            posts.insert(followed, "followed-" + i, i);
        }
        posts.insert(kindred, "kindred", 1000);

        ResultActions first = feed(null).andExpect(jsonPath("$.data.items", hasSize(20)));

        feed(posts.nextCursor(first))
                .andExpect(jsonPath("$.data.items[*].body",
                        contains("followed-20", "followed-21", "followed-22", "followed-23", "followed-24", "kindred")));
    }

    @Test
    void aTamperedCursorIsABadRequest() throws Exception {
        feed("garbage").andExpect(status().isBadRequest());
        feed("7.").andExpect(status().isBadRequest());
        feed("0.not-base64!").andExpect(status().isBadRequest());
    }

    private ResultActions feed(String cursor) throws Exception {
        var request = get("/feed").header(HttpHeaders.AUTHORIZATION, bearer);
        return mockMvc.perform(cursor == null ? request : request.param("cursor", cursor));
    }
}
