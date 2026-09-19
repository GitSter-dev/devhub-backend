package com.application.devhub.auth;

import com.application.devhub.IntegrationTest;
import com.application.devhub.user.User;
import com.application.devhub.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SignupIntegrationTest extends IntegrationTest {

    private static final String PASSWORD = "supersecret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Test
    void createsUnverifiedUserWithoutIssuingTokens() throws Exception {
        signup("ada_dev", "Ada Lovelace", "  Ada@Dev.io ", PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.username").value("ada_dev"))
                .andExpect(jsonPath("$.data.displayName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.data.email").value("ada@dev.io"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.password").doesNotExist());

        User stored = userRepository.findByLogin("ada@dev.io").orElseThrow();
        assertThat(stored.getEmail()).isEqualTo("ada@dev.io");
        assertThat(stored.isEmailVerified()).isFalse();
        assertThat(stored.getPasswordHash()).startsWith("{bcrypt}");
        assertThat(passwordEncoder.matches(PASSWORD, stored.getPasswordHash())).isTrue();
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() throws Exception {
        signup("ada_dev", "Ada", "ada@dev.io", PASSWORD).andExpect(status().isCreated());

        signup("someone_else", "Someone", "ADA@dev.io", PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_TAKEN"));
    }

    @Test
    void rejectsDuplicateUsernameIgnoringCase() throws Exception {
        signup("ada_dev", "Ada", "ada@dev.io", PASSWORD).andExpect(status().isCreated());

        signup("ADA_DEV", "Another Ada", "other@dev.io", PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("USERNAME_TAKEN"));
    }

    @Test
    void rejectsInvalidBodyWithFieldErrors() throws Exception {
        signup("ada@dev", "   ", "not-an-email", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.fieldErrors.username").exists())
                .andExpect(jsonPath("$.error.fieldErrors.displayName").exists())
                .andExpect(jsonPath("$.error.fieldErrors.email").exists())
                .andExpect(jsonPath("$.error.fieldErrors.password").exists());
    }

    @Test
    void newUserCannotLogInBeforeVerification() throws Exception {
        signup("ada_dev", "Ada", "ada@dev.io", PASSWORD).andExpect(status().isCreated());

        assertThatThrownBy(() -> authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("ada_dev", PASSWORD)))
                .isInstanceOf(DisabledException.class);
    }

    private ResultActions signup(String username, String displayName, String email, String password) throws Exception {
        String body = """
                {"username":"%s","displayName":"%s","email":"%s","password":"%s"}
                """.formatted(username, displayName, email, password);
        return mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
