package com.application.devhub.post;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostIntegrationTest extends IntegrationTest {

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
    void postsCanBeTextCodeOrBothAndTheLanguageIsNormalized() throws Exception {
        posts.create(ada, "{\"body\": \"  hello  \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.body").value("hello"))
                .andExpect(jsonPath("$.data.author.username").value("ada"))
                .andExpect(jsonPath("$.data.mine").value(true))
                .andExpect(jsonPath("$.data.replyToId").doesNotExist());

        posts.create(ada, "{\"code\": \"  fn main() {}  \", \"codeLanguage\": \"Rust\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.body").doesNotExist())
                .andExpect(jsonPath("$.data.code").value("  fn main() {}"))
                .andExpect(jsonPath("$.data.codeLanguage").value("rust"));
    }

    @Test
    void emptyOversizedAndBadlyLabelledPostsAreRejected() throws Exception {
        posts.create(ada, "{\"body\": \"   \", \"code\": \"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors.bodyOrCodePresent").value("write something or add a code block"));
        posts.create(ada, "{\"body\": \"" + "x".repeat(501) + "\"}")
                .andExpect(jsonPath("$.error.fieldErrors.body").exists());
        posts.create(ada, "{\"code\": \"x\", \"codeLanguage\": \"c sharp\"}")
                .andExpect(jsonPath("$.error.fieldErrors.codeLanguage").exists());
    }

    @Test
    void repliesChainBackToTheirRoot() throws Exception {
        UUID root = posts.publish(ada, "root");
        UUID first = posts.reply(grace, root, "first");

        posts.create(ada, "{\"body\": \"second\", \"replyToId\": \"" + first + "\"}")
                .andExpect(jsonPath("$.data.replyToId").value(first.toString()))
                .andExpect(jsonPath("$.data.replyToUsername").value("grace"))
                .andExpect(jsonPath("$.data.rootId").value(root.toString()));
    }

    @Test
    void replyingToAMissingOrDeletedPostIsNotFound() throws Exception {
        UUID root = posts.publish(ada, "root");
        deletePost(ada, root);

        posts.create(grace, "{\"body\": \"hi\", \"replyToId\": \"" + root + "\"}").andExpect(status().isNotFound());
        posts.create(grace, "{\"body\": \"hi\", \"replyToId\": \"" + UUID.randomUUID() + "\"}").andExpect(status().isNotFound());
    }

    @Test
    void onlyTheAuthorCanDeleteAndDeletingTwiceIsHarmless() throws Exception {
        UUID post = posts.publish(ada, "mine");

        deletePost(grace, post).andExpect(status().isForbidden());
        deletePost(ada, post).andExpect(status().isOk());
        deletePost(ada, post).andExpect(status().isOk());
    }

    @Test
    void repliesToADeletedPostStayRepliesAndNeverBecomeOriginals() throws Exception {
        UUID root = posts.publish(ada, "root");
        UUID reply = posts.reply(grace, root, "reply");
        deletePost(ada, root);

        mockMvc.perform(get("/posts/{id}", reply).header(HttpHeaders.AUTHORIZATION, grace))
                .andExpect(jsonPath("$.data.post.replyToDeleted").value(true))
                .andExpect(jsonPath("$.data.post.rootId").value(root.toString()))
                .andExpect(jsonPath("$.data.ancestors[0].deleted").value(true))
                .andExpect(jsonPath("$.data.ancestors[0].body").doesNotExist());
        mockMvc.perform(get("/users/grace/posts").header(HttpHeaders.AUTHORIZATION, grace))
                .andExpect(jsonPath("$.data.items", empty()));
        mockMvc.perform(get("/feed").header(HttpHeaders.AUTHORIZATION, grace))
                .andExpect(jsonPath("$.data.items", empty()));
    }

    @Test
    void aPostWithRepliesCanNeverBeHardDeleted() throws Exception {
        UUID root = posts.publish(ada, "root");
        posts.reply(grace, root, "reply");

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM posts WHERE id = ?", root))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void likesAreCountedOnceAndCanBeRemoved() throws Exception {
        UUID post = posts.publish(ada, "likeable");

        like(grace, post).andExpect(status().isOk());
        like(grace, post).andExpect(status().isOk());
        view(grace, post).andExpect(jsonPath("$.data.post.likeCount").value(1))
                .andExpect(jsonPath("$.data.post.liked").value(true));

        unlike(grace, post).andExpect(status().isOk());
        unlike(grace, post).andExpect(status().isOk());
        view(grace, post).andExpect(jsonPath("$.data.post.likeCount").value(0))
                .andExpect(jsonPath("$.data.post.liked").value(false));
    }

    @Test
    void deletedPostsCannotBeLiked() throws Exception {
        UUID post = posts.publish(ada, "gone");
        deletePost(ada, post);

        like(grace, post).andExpect(status().isNotFound());
    }

    private ResultActions deletePost(String bearer, UUID post) throws Exception {
        return mockMvc.perform(delete("/posts/{id}", post)
                .header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private ResultActions like(String bearer, UUID post) throws Exception {
        return mockMvc.perform(put("/posts/{id}/like", post).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private ResultActions unlike(String bearer, UUID post) throws Exception {
        return mockMvc.perform(delete("/posts/{id}/like", post).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private ResultActions view(String bearer, UUID post) throws Exception {
        return mockMvc.perform(get("/posts/{id}", post).header(HttpHeaders.AUTHORIZATION, bearer));
    }
}
