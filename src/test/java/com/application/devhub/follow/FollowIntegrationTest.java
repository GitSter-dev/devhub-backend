package com.application.devhub.follow;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FollowIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID ada;
    private UUID grace;
    private String bearer;

    @BeforeEach
    void signIn() throws Exception {
        ada = users.verified("ada");
        grace = users.verified("grace");
        bearer = users.bearer("ada");
    }

    @Test
    void followingIsRecordedOnceEvenWhenRepeated() throws Exception {
        follow(grace).andExpect(status().isOk());
        follow(grace).andExpect(status().isOk());

        assertThat(follows(ada, grace)).isEqualTo(1);
    }

    @Test
    void unfollowingRemovesTheFollowAndRepeatingIsHarmless() throws Exception {
        follow(grace);

        unfollow(grace).andExpect(status().isOk());
        unfollow(grace).andExpect(status().isOk());

        assertThat(follows(ada, grace)).isZero();
    }

    @Test
    void followingYourselfIsRejected() throws Exception {
        follow(ada)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CANNOT_FOLLOW_SELF"));
    }

    @Test
    void followingAnUnknownUserIsNotFound() throws Exception {
        follow(UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void followingAnUnverifiedUserIsNotFound() throws Exception {
        follow(users.unverified("ghost")).andExpect(status().isNotFound());
    }

    private ResultActions follow(UUID userId) throws Exception {
        return mockMvc.perform(put("/users/me/following/{userId}", userId).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private ResultActions unfollow(UUID userId) throws Exception {
        return mockMvc.perform(delete("/users/me/following/{userId}", userId).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private Integer follows(UUID follower, UUID followee) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM follows WHERE follower_id = ? AND followee_id = ?",
                Integer.class, follower, followee);
    }
}
