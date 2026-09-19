package com.application.devhub.passwordreset;

import com.application.devhub.IntegrationTest;
import com.application.devhub.otp.OneTimeCodeIssuer;
import com.application.devhub.otp.OneTimeCodeProperties;
import com.application.devhub.otp.OneTimeCodePurpose;
import com.application.devhub.otp.OneTimeCodeRepository;
import com.application.devhub.outbox.OutboxEventRepository;
import com.application.devhub.user.User;
import com.application.devhub.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordResetIntegrationTest extends IntegrationTest {

    private static final OneTimeCodePurpose PURPOSE = OneTimeCodePurpose.PASSWORD_RESET;
    private static final String EMAIL = "linus@dev.io";
    private static final String NEW_PASSWORD = "brand-new-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OneTimeCodeIssuer codeIssuer;

    @Autowired
    private OneTimeCodeRepository codeRepository;

    @Autowired
    private OneTimeCodeProperties properties;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    private User linus;

    @BeforeEach
    void createUsers() {
        linus = new User("linus_t", "Linus Torvalds", EMAIL, passwordEncoder.encode("old-password"));
        linus.markEmailVerified();
        linus = userRepository.save(linus);
        userRepository.save(new User("grace", "Grace Hopper", "grace@dev.io", passwordEncoder.encode("old-password")));
    }

    @Test
    void unknownAndUnverifiedEmailsQueueNothing() throws Exception {
        forgotPassword("nobody@dev.io").andExpect(status().isAccepted());
        forgotPassword("grace@dev.io").andExpect(status().isAccepted());

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void wrongCodeAndUnknownEmailAreIndistinguishable() throws Exception {
        String code = codeIssuer.issue(linus, PURPOSE);

        JsonNode wrongCode = errorOf(reset(EMAIL, otherThan(code), NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_RESET_CODE")));
        JsonNode unknownEmail = errorOf(reset("nobody@dev.io", code, NEW_PASSWORD));

        assertThat(unknownEmail).isEqualTo(wrongCode);
    }

    @Test
    void dailyLimitStopsFurtherCodes() throws Exception {
        int dailyLimit = properties.passwordReset().dailyLimit();
        for (int i = 0; i < dailyLimit - 1; i++) {
            issueOutsideCooldown();
        }
        forgotPassword(EMAIL).andExpect(status().isAccepted());
        assertThat(outboxRepository.count()).isEqualTo(1);

        issueOutsideCooldown();
        forgotPassword(EMAIL).andExpect(status().isAccepted());
        assertThat(outboxRepository.count()).isEqualTo(1);
    }

    @Test
    void burningACodeKeepsItsIssueCount() throws Exception {
        String code = codeIssuer.issue(linus, PURPOSE);
        for (int i = 0; i < properties.passwordReset().maxAttempts(); i++) {
            reset(EMAIL, otherThan(code), NEW_PASSWORD).andExpect(status().isBadRequest());
        }

        reset(EMAIL, code, NEW_PASSWORD).andExpect(status().isBadRequest());
        assertThat(codeRepository.findByUserIdAndPurpose(linus.getId(), PURPOSE)).hasValueSatisfying(stored -> {
            assertThat(stored.isActive()).isFalse();
            assertThat(stored.getIssuesInWindow()).isEqualTo(1);
        });
    }

    @Test
    void weakNewPasswordIsRejected() throws Exception {
        String code = codeIssuer.issue(linus, PURPOSE);

        reset(EMAIL, code, "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.fieldErrors.newPassword").exists());
    }

    private void issueOutsideCooldown() {
        codeIssuer.issue(linus, PURPOSE);
        jdbcTemplate.update("UPDATE one_time_codes SET issued_at = now() - interval '1 hour'");
    }

    private ResultActions forgotPassword(String email) throws Exception {
        return mockMvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email)));
    }

    private ResultActions reset(String email, String code, String newPassword) throws Exception {
        return mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"code\":\"%s\",\"newPassword\":\"%s\"}".formatted(email, code, newPassword)));
    }

    private JsonNode errorOf(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("error");
    }

    private static String otherThan(String code) {
        return code.equals("000000") ? "000001" : "000000";
    }
}
