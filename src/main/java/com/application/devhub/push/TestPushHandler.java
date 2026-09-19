package com.application.devhub.push;

import com.application.devhub.outbox.OutboxEvent;
import com.application.devhub.outbox.OutboxEventHandler;
import com.application.devhub.outbox.OutboxEventType;
import com.application.devhub.user.UserEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TestPushHandler implements OutboxEventHandler {

    private static final String KIND = "test";

    private final JsonMapper jsonMapper;
    private final PushDispatcher pushDispatcher;
    private final MessageSource messageSource;
    private final PushProperties properties;

    @Override
    public OutboxEventType type() {
        return OutboxEventType.PUSH_TEST;
    }

    @Override
    public void handle(OutboxEvent event) {
        UserEventPayload payload = jsonMapper.readValue(event.getPayload(), UserEventPayload.class);
        pushDispatcher.dispatch(payload.userId(), message());
    }

    private PushMessage message() {
        return new PushMessage(
                messageSource.getMessage("push.test.title", null, properties.locale()),
                messageSource.getMessage("push.test.body", null, properties.locale()),
                Map.of("kind", KIND));
    }
}
