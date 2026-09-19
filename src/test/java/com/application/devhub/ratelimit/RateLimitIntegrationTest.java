package com.application.devhub.ratelimit;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import com.application.devhub.user.User;
import com.application.devhub.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "devhub.rate-limit.enabled=true",
        "devhub.rate-limit.limits.login.capacity=3",
        "devhub.rate-limit.limits.login.period=1h",
        "devhub.rate-limit.limits.login-failures.capacity=3",
        "devhub.rate-limit.limits.login-failures.period=1h",
        "devhub.rate-limit.limits.verify-email.capacity=3",
        "devhub.rate-limit.limits.verify-email.period=1h",
        "devhub.rate-limit.limits.follow.capacity=3",
        "devhub.rate-limit.limits.follow.period=1h"
})
class RateLimitIntegrationTest extends IntegrationTest {

    private static final String PASSWORD = "supersecret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private TestUsers users;

    @Test
    void theFollowLimitIsPerAccountSoSharedNetworksDontCollide() throws Exception {
        UUID target = users.verified("follow_target");
        users.verified("follower_one");
        users.verified("follower_two");
        String one = users.bearer("follower_one");
        String two = users.bearer("follower_two");

        for (int i = 0; i < 3; i++) {
            follow("10.0.9.1", one, target).andExpect(status().isOk());
        }

        follow("10.0.9.1", one, target)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, not("0")));
        follow("10.0.9.1", two, target).andExpect(status().isOk());
    }

    private ResultActions follow(String ip, String bearer, UUID target) throws Exception {
        return mockMvc.perform(put("/users/me/following/{userId}", target)
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .header(HttpHeaders.AUTHORIZATION, bearer));
    }

    @Test
    void clientOverTheIpLimitGets429WithRetryAfter() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("10.0.0.1", "someone" + i, PASSWORD).andExpect(status().isUnauthorized());
        }

        login("10.0.0.1", "someone-else", PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, not("0")))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void otherClientsAreUnaffected() throws Exception {
        for (int i = 0; i < 4; i++) {
            login("10.0.1.1", "someone" + i, PASSWORD);
        }

        login("10.0.1.2", "someone", PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void failedLoginsOnOneAccountAreCappedAcrossIps() throws Exception {
        createVerifiedUser("target");
        for (int i = 1; i <= 3; i++) {
            login("10.0.2." + i, "target", "wrong-password").andExpect(status().isUnauthorized());
        }

        login("10.0.2.4", "TARGET", PASSWORD).andExpect(status().isTooManyRequests());
    }

    @Test
    void successfulLoginsDoNotConsumeTheAccountLimit() throws Exception {
        createVerifiedUser("regular");
        for (int i = 1; i <= 4; i++) {
            login("10.0.3." + i, "regular", PASSWORD).andExpect(status().isOk());
        }
    }

    @Test
    void lockedOutUnknownAndRealAccountsLookTheSame() throws Exception {
        createVerifiedUser("real");
        for (int i = 1; i <= 3; i++) {
            login("10.0.4." + i, "real", "wrong-password");
            login("10.0.5." + i, "ghost", "wrong-password");
        }

        JsonNode real = errorOf(login("10.0.4.9", "real", PASSWORD).andExpect(status().isTooManyRequests()));
        JsonNode ghost = errorOf(login("10.0.5.9", "ghost", PASSWORD).andExpect(status().isTooManyRequests()));

        assertThat(ghost).isEqualTo(real);
    }

    @Test
    void codeEndpointsAreLimitedPerIp() throws Exception {
        for (int i = 0; i < 3; i++) {
            verifyEmail("10.0.6.1").andExpect(status().isBadRequest());
        }

        verifyEmail("10.0.6.1").andExpect(status().isTooManyRequests());
    }

    private void createVerifiedUser(String username) {
        User user = new User(username, username, username + "@dev.io", passwordEncoder.encode(PASSWORD));
        user.markEmailVerified();
        userRepository.save(user);
    }

    private ResultActions login(String clientIp, String identifier, String password) throws Exception {
        return postFrom(clientIp, "/auth/login",
                "{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(identifier, password));
    }

    private ResultActions verifyEmail(String clientIp) throws Exception {
        return postFrom(clientIp, "/auth/verify-email", "{\"email\":\"nobody@dev.io\",\"code\":\"123456\"}");
    }

    private ResultActions postFrom(String clientIp, String path, String body) throws Exception {
        return mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(request -> {
                    request.setRemoteAddr(clientIp);
                    return request;
                }));
    }

    private JsonNode errorOf(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("error");
    }
}
