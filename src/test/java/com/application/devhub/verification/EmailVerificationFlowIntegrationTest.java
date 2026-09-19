package com.application.devhub.verification;

import com.application.devhub.IntegrationTest;
import com.application.devhub.MailpitClient;
import com.application.devhub.MailpitClient.MessageSummary;
import com.application.devhub.mail.EmailTemplate;
import com.application.devhub.mail.MailProperties;
import com.application.devhub.otp.OneTimeCodeRepository;
import com.application.devhub.outbox.OutboxEvent;
import com.application.devhub.outbox.OutboxEventRepository;
import com.application.devhub.outbox.OutboxEventType;
import com.application.devhub.outbox.OutboxRelay;
import com.application.devhub.outbox.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmailVerificationFlowIntegrationTest extends IntegrationTest {

    private static final Pattern CODE_IN_EMAIL = Pattern.compile("class=\"code\">(\\d{6})<");
    private static final String PASSWORD = "supersecret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MailpitClient mailpit;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private OneTimeCodeRepository codeRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private MailProperties mailProperties;

    @Autowired
    private JsonMapper jsonMapper;

    @BeforeEach
    void clearMailbox() {
        mailpit.clear();
    }

    @Test
    void signupSendsStyledCodeThatVerifiesTheAccount() throws Exception {
        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"ada_dev","displayName":"Ada Lovelace","email":"ada@dev.io","password":"%s"}
                        """.formatted(PASSWORD)))
                .andExpect(status().isCreated());

        assertThat(outboxRepository.findAll()).singleElement()
                .satisfies(event -> assertThat(event.getType()).isEqualTo(OutboxEventType.EMAIL_VERIFICATION))
                .satisfies(event -> assertThat(jsonMapper.readTree(event.getPayload()).propertyNames())
                        .containsExactly("userId"));
        assertThat(mailpit.messages()).isEmpty();

        outboxRelay.relayPending();

        List<MessageSummary> messages = mailpit.messages();
        assertThat(messages).singleElement()
                .satisfies(message -> assertThat(message.to()).extracting(MailpitClient.Recipient::address)
                        .containsExactly("ada@dev.io"))
                .satisfies(message -> assertThat(message.subject()).isEqualTo(messageSource.getMessage(
                        EmailTemplate.VERIFICATION.subjectKey(), null, mailProperties.locale())));

        String html = mailpit.message(messages.getFirst().id()).html();
        assertThat(html).contains(".window-bar").contains(".terminal").contains("Ada Lovelace");
        Matcher matcher = CODE_IN_EMAIL.matcher(html);
        assertThat(matcher.find()).isTrue();

        mockMvc.perform(post("/auth/verify-email").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"ADA@dev.io","code":"%s"}
                        """.formatted(matcher.group(1))))
                .andExpect(status().isOk());

        assertThat(outboxRepository.findAll()).extracting(OutboxEvent::getStatus).containsExactly(OutboxStatus.SENT);
        assertThat(codeRepository.findAll()).singleElement().satisfies(code -> assertThat(code.isActive()).isFalse());
        assertThat(authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("ada_dev", PASSWORD)).isAuthenticated()).isTrue();
    }
}
