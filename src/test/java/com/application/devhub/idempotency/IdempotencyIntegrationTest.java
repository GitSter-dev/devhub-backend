package com.application.devhub.idempotency;

import com.application.devhub.IntegrationTest;
import com.application.devhub.outbox.OutboxEventRepository;
import com.application.devhub.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IdempotencyIntegrationTest extends IntegrationTest {

    private static final String KEY = "signup-key-0001";
    private static final String SIGNUP = """
            {"username":"ada_dev","displayName":"Ada","email":"ada@dev.io","password":"supersecret"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void retryWithTheSameKeyReplaysTheOriginalResponse() throws Exception {
        MvcResult first = signup(KEY, SIGNUP)
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist(Idempotent.REPLAYED_HEADER))
                .andReturn();

        MvcResult replay = signup(KEY, SIGNUP)
                .andExpect(status().isCreated())
                .andExpect(header().string(Idempotent.REPLAYED_HEADER, "true"))
                .andReturn();

        assertThat(replay.getResponse().getContentAsString()).isEqualTo(first.getResponse().getContentAsString());
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(outboxRepository.count()).isEqualTo(1);
    }

    @Test
    void reusingAKeyWithADifferentBodyIsRejected() throws Exception {
        signup(KEY, SIGNUP).andExpect(status().isCreated());

        signup(KEY, SIGNUP.replace("ada_dev", "grace"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    void requestStillInProgressIsReportedAsSuch() throws Exception {
        signup(KEY, SIGNUP).andExpect(status().isCreated());
        jdbcTemplate.update("UPDATE idempotency_records SET status = 'IN_PROGRESS', response_status = NULL");

        signup(KEY, SIGNUP)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_IN_PROGRESS"));
    }

    @Test
    void withoutAKeyRequestsRunEveryTime() throws Exception {
        signup(null, SIGNUP).andExpect(status().isCreated());

        signup(null, SIGNUP)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_TAKEN"));
    }

    @Test
    void deterministicClientErrorsAreReplayedToo() throws Exception {
        String invalid = "{\"username\":\"a@b\",\"displayName\":\"\",\"email\":\"x\",\"password\":\"1\"}";
        signup(KEY, invalid).andExpect(status().isBadRequest());

        signup(KEY, invalid)
                .andExpect(status().isBadRequest())
                .andExpect(header().string(Idempotent.REPLAYED_HEADER, "true"))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void malformedKeyIsRejected() throws Exception {
        signup("bad key!", SIGNUP)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void keysAreIgnoredOnEndpointsThatAreNotIdempotent() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .header(Idempotent.HEADER, KEY)
                        .content("{\"identifier\":\"nobody\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM idempotency_records", Integer.class)).isZero();
    }

    private ResultActions signup(String key, String body) throws Exception {
        var request = post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body);
        if (key != null) {
            request.header(Idempotent.HEADER, key);
        }
        return mockMvc.perform(request);
    }
}
