package com.application.devhub.user;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsernameChangeIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String ada;
    private String grace;

    @BeforeEach
    void signIn() throws Exception {
        users.verified("ada");
        users.verified("grace");
        ada = users.bearer("ada");
        grace = users.bearer("grace");
    }

    @Test
    void renamingSwitchesLoginAndKeepsTheOldLinkWorking() throws Exception {
        change(ada, "ada_builds")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("ada_builds"))
                .andExpect(jsonPath("$.data.usernameChangeAvailableAt").exists());

        login("ada").andExpect(status().isUnauthorized());
        login("ada_builds").andExpect(status().isOk());
        mockMvc.perform(get("/users/ada/profile").header(HttpHeaders.AUTHORIZATION, grace))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("ada_builds"));
    }

    @Test
    void aSecondChangeWithinTheCooldownIsRejected() throws Exception {
        change(ada, "ada_builds");

        change(ada, "ada_again")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("USERNAME_CHANGE_TOO_SOON"))
                .andExpect(jsonPath("$.error.message", startsWith("You can change your username again after")));
    }

    @Test
    void theCooldownEndsAfterItsPeriod() throws Exception {
        change(ada, "ada_builds");
        jdbcTemplate.update("UPDATE users SET username_changed_at = now() - interval '31 days' WHERE username = 'ada_builds'");

        change(ada, "ada_again").andExpect(status().isOk());
    }

    @Test
    void takingBackYourOwnHeldHandleSkipsTheCooldown() throws Exception {
        change(ada, "ada_builds");

        change(ada, "ada").andExpect(status().isOk()).andExpect(jsonPath("$.data.username").value("ada"));
    }

    @Test
    void someoneElsesCurrentOrHeldHandleIsTaken() throws Exception {
        change(ada, "GRACE").andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("USERNAME_TAKEN"));

        change(grace, "grace_codes");
        jdbcTemplate.update("UPDATE users SET username_changed_at = now() - interval '31 days'");

        change(ada, "grace").andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("USERNAME_TAKEN"));
    }

    @Test
    void aHeldHandleCannotBeRegisteredAtSignup() throws Exception {
        change(ada, "ada_builds");

        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "ADA", "displayName": "Imposter", "email": "imposter@dev.io",
                         "password": "correct-horse-battery"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("USERNAME_TAKEN"));
    }

    @Test
    void aHandleIsFreeAgainOnceItsHoldExpires() throws Exception {
        change(grace, "grace_codes");
        jdbcTemplate.update("UPDATE held_usernames SET held_until = now() - interval '1 second'");

        change(ada, "grace").andExpect(status().isOk());
    }

    @Test
    void availabilityExplainsWhyAHandleCantBeUsed() throws Exception {
        change(grace, "grace_codes");

        availability(ada, "x!").andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.reason").value("INVALID"));
        availability(ada, "grace").andExpect(jsonPath("$.data.reason").value("TAKEN"));
        availability(ada, "grace_codes").andExpect(jsonPath("$.data.reason").value("TAKEN"));
        availability(ada, "ADA").andExpect(jsonPath("$.data.available").value(true));
        availability(ada, "brand_new").andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.reason").doesNotExist());
    }

    private ResultActions change(String bearer, String username) throws Exception {
        return mockMvc.perform(put("/users/me/username").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"username\": \"" + username + "\"}"));
    }

    private ResultActions availability(String bearer, String username) throws Exception {
        return mockMvc.perform(get("/users/username-availability").param("username", username)
                .header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private ResultActions login(String identifier) throws Exception {
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(identifier, TestUsers.PASSWORD)));
    }
}
