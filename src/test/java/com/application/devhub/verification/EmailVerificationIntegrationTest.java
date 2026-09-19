package com.application.devhub.verification;

import com.application.devhub.IntegrationTest;
import com.application.devhub.otp.OneTimeCodeIssuer;
import com.application.devhub.otp.OneTimeCodeProperties;
import com.application.devhub.otp.OneTimeCodePurpose;
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

class EmailVerificationIntegrationTest extends IntegrationTest {

    private static final String EMAIL = "grace@dev.io";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OneTimeCodeIssuer codeIssuer;

    @Autowired
    private OneTimeCodeProperties properties;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    private User user;

    @BeforeEach
    void createUnverifiedUser() {
        user = userRepository.save(new User("grace", "Grace Hopper", EMAIL, passwordEncoder.encode("supersecret")));
    }

    @Test
    void wrongCodeIsRejected() throws Exception {
        String code = codeIssuer.issue(user, OneTimeCodePurpose.EMAIL_VERIFICATION);

        verify(EMAIL, otherThan(code))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_VERIFICATION_CODE"));
    }

    @Test
    void codeIsBurnedAfterMaxAttempts() throws Exception {
        String code = codeIssuer.issue(user, OneTimeCodePurpose.EMAIL_VERIFICATION);
        for (int i = 0; i < properties.emailVerification().maxAttempts(); i++) {
            verify(EMAIL, otherThan(code)).andExpect(status().isBadRequest());
        }

        verify(EMAIL, code).andExpect(status().isBadRequest());
        assertThat(userRepository.findById(user.getId()).orElseThrow().isEmailVerified()).isFalse();
    }

    @Test
    void expiredCodeIsRejected() throws Exception {
        String code = codeIssuer.issue(user, OneTimeCodePurpose.EMAIL_VERIFICATION);
        jdbcTemplate.update("UPDATE one_time_codes SET expires_at = now() - interval '1 minute'");

        verify(EMAIL, code).andExpect(status().isBadRequest());
    }

    @Test
    void unknownAndVerifiedEmailsAreIndistinguishableFromWrongCode() throws Exception {
        String code = codeIssuer.issue(user, OneTimeCodePurpose.EMAIL_VERIFICATION);
        JsonNode wrongCode = errorOf(verify(EMAIL, otherThan(code)));

        JsonNode unknownEmail = errorOf(verify("nobody@dev.io", code));

        User verified = new User("ada", "Ada", "ada@dev.io", passwordEncoder.encode("supersecret"));
        verified.markEmailVerified();
        userRepository.save(verified);
        JsonNode verifiedEmail = errorOf(verify("ada@dev.io", code));

        assertThat(unknownEmail).isEqualTo(wrongCode);
        assertThat(verifiedEmail).isEqualTo(wrongCode);
    }

    @Test
    void resendQueuesEmailOnlyForUnverifiedAccountsOffCooldown() throws Exception {
        resend("nobody@dev.io").andExpect(status().isAccepted());
        assertThat(outboxRepository.count()).isZero();

        resend(EMAIL).andExpect(status().isAccepted());
        assertThat(outboxRepository.count()).isEqualTo(1);

        codeIssuer.issue(user, OneTimeCodePurpose.EMAIL_VERIFICATION);
        resend(EMAIL).andExpect(status().isAccepted());
        assertThat(outboxRepository.count()).isEqualTo(1);

        User verified = new User("ada", "Ada", "ada@dev.io", passwordEncoder.encode("supersecret"));
        verified.markEmailVerified();
        userRepository.save(verified);
        resend("ada@dev.io").andExpect(status().isAccepted());
        assertThat(outboxRepository.count()).isEqualTo(1);
    }

    private ResultActions verify(String email, String code) throws Exception {
        return mockMvc.perform(post("/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"code\":\"%s\"}".formatted(email, code)));
    }

    private ResultActions resend(String email) throws Exception {
        return mockMvc.perform(post("/auth/resend-verification").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email)));
    }

    private JsonNode errorOf(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("error");
    }

    private static String otherThan(String code) {
        return code.equals("000000") ? "000001" : "000000";
    }
}
