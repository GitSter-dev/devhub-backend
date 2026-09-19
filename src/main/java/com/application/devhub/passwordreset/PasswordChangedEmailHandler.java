package com.application.devhub.passwordreset;

import com.application.devhub.mail.EmailRenderer;
import com.application.devhub.mail.EmailSender;
import com.application.devhub.mail.EmailTemplate;
import com.application.devhub.outbox.OutboxEvent;
import com.application.devhub.outbox.OutboxEventHandler;
import com.application.devhub.outbox.OutboxEventType;
import com.application.devhub.user.User;
import com.application.devhub.user.UserEventPayload;
import com.application.devhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PasswordChangedEmailHandler implements OutboxEventHandler {

    private final JsonMapper jsonMapper;
    private final UserRepository userRepository;
    private final EmailRenderer renderer;
    private final EmailSender sender;

    @Override
    public OutboxEventType type() {
        return OutboxEventType.PASSWORD_CHANGED;
    }

    @Override
    public void handle(OutboxEvent event) {
        UserEventPayload payload = jsonMapper.readValue(event.getPayload(), UserEventPayload.class);
        userRepository.findById(payload.userId()).ifPresent(this::sendNotice);
    }

    private void sendNotice(User user) {
        Map<String, Object> variables = Map.of("displayName", user.getDisplayName());
        sender.send(user.getEmail(), renderer.render(EmailTemplate.PASSWORD_CHANGED, variables));
    }
}
