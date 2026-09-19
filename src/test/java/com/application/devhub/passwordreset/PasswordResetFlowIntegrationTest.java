package com.application.devhub.passwordreset;

import com.application.devhub.IntegrationTest;
import com.application.devhub.MailpitClient;
import com.application.devhub.MailpitClient.MessageSummary;
import com.application.devhub.mail.EmailTemplate;
import com.application.devhub.mail.MailProperties;
import com.application.devhub.outbox.OutboxEvent;
import com.application.devhub.outbox.OutboxEventRepository;
import com.application.devhub.outbox.OutboxEventType;
import com.application.devhub.outbox.OutboxRelay;
import com.application.devhub.user.User;
import com.application.devhub.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordResetFlowIntegrationTest extends IntegrationTest {

    private static final Pattern CODE_IN_EMAIL = Pattern.compile("class=\"code\">(\\d{6})<");
    private static final String OLD_PASSWORD = "old-password";
    private static final String NEW_PASSWORD = "brand-new-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MailpitClient mailpit;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private MailProperties mailProperties;

    @Autowired
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        mailpit.clear();
        User linus = new User("linus_t", "Linus Torvalds", "linus@dev.io", passwordEncoder.encode(OLD_PASSWORD));
        linus.markEmailVerified();
        userRepository.save(linus);
    }

    @Test
    void emailedCodeResetsPasswordRevokesSessionsAndSendsNotice() throws Exception {
        String oldRefreshToken = jsonMapper.readTree(login(OLD_PASSWORD).andReturn().getResponse().getContentAsString())
                .get("data").get("refreshToken").asString();

        postJson("/auth/forgot-password", "{\"email\":\"Linus@Dev.io\"}").andExpect(status().isAccepted());
        assertThat(outboxRepository.findAll()).extracting(OutboxEvent::getType)
                .containsExactly(OutboxEventType.PASSWORD_RESET);

        outboxRelay.relayPending();

        MessageSummary resetEmail = mailpit.messages().getFirst();
        assertThat(resetEmail.subject()).isEqualTo(subjectOf(EmailTemplate.PASSWORD_RESET));
        Matcher matcher = CODE_IN_EMAIL.matcher(mailpit.message(resetEmail.id()).html());
        assertThat(matcher.find()).isTrue();

        postJson("/auth/reset-password", """
                {"email":"linus@dev.io","code":"%s","newPassword":"%s"}
                """.formatted(matcher.group(1), NEW_PASSWORD))
                .andExpect(status().isOk());

        login(OLD_PASSWORD).andExpect(status().isUnauthorized());
        login(NEW_PASSWORD).andExpect(status().isOk());
        postJson("/auth/refresh", "{\"refreshToken\":\"%s\"}".formatted(oldRefreshToken))
                .andExpect(status().isUnauthorized());

        mailpit.clear();
        outboxRelay.relayPending();

        assertThat(mailpit.messages()).singleElement()
                .satisfies(notice -> assertThat(notice.subject()).isEqualTo(subjectOf(EmailTemplate.PASSWORD_CHANGED)))
                .satisfies(notice -> assertThat(mailpit.message(notice.id()).html())
                        .contains("all sessions revoked")
                        .contains(".log-tag"));
    }

    private ResultActions login(String password) throws Exception {
        return postJson("/auth/login", "{\"identifier\":\"linus_t\",\"password\":\"%s\"}".formatted(password));
    }

    private ResultActions postJson(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String subjectOf(EmailTemplate template) {
        return messageSource.getMessage(template.subjectKey(), null, mailProperties.locale());
    }
}
