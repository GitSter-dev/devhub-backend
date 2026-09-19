package com.application.devhub.user;

import com.application.devhub.IntegrationTest;
import com.application.devhub.security.AuthUser;
import com.application.devhub.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CurrentUserIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    @Test
    void returnsTheSignedInUser() throws Exception {
        User user = new User("ada_dev", "Ada Lovelace", "ada@dev.io", "{noop}x");
        user.markEmailVerified();
        user = userRepository.save(user);

        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearerFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.data.username").value("ada_dev"))
                .andExpect(jsonPath("$.data.displayName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.data.email").value("ada@dev.io"))
                .andExpect(jsonPath("$.data.emailVerified").value(true))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void requiresAToken() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void deletedAccountIsNotFound() throws Exception {
        User user = userRepository.save(new User("gone", "Gone", "gone@dev.io", "{noop}x"));
        String bearer = bearerFor(user);
        userRepository.delete(user);

        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    private String bearerFor(User user) {
        return "Bearer " + tokenService.issueAccessToken(AuthUser.from(user), UUID.randomUUID()).value();
    }
}
