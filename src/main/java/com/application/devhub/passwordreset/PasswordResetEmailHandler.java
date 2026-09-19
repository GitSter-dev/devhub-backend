package com.application.devhub.passwordreset;

import com.application.devhub.mail.EmailTemplate;
import com.application.devhub.otp.CodeEmailSender;
import com.application.devhub.otp.OneTimeCodePurpose;
import com.application.devhub.outbox.OutboxEvent;
import com.application.devhub.outbox.OutboxEventHandler;
import com.application.devhub.outbox.OutboxEventType;
import com.application.devhub.user.User;
import com.application.devhub.user.UserEventPayload;
import com.application.devhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class PasswordResetEmailHandler implements OutboxEventHandler {

    private final JsonMapper jsonMapper;
    private final UserRepository userRepository;
    private final CodeEmailSender codeEmailSender;

    @Override
    public OutboxEventType type() {
        return OutboxEventType.PASSWORD_RESET;
    }

    @Override
    public void handle(OutboxEvent event) {
        UserEventPayload payload = jsonMapper.readValue(event.getPayload(), UserEventPayload.class);
        userRepository.findById(payload.userId())
                .filter(User::isEmailVerified)
                .ifPresent(user -> codeEmailSender.send(user, OneTimeCodePurpose.PASSWORD_RESET,
                        EmailTemplate.PASSWORD_RESET));
    }
}
