package com.application.devhub.push;

import com.application.devhub.outbox.OutboxEvent;
import com.application.devhub.outbox.OutboxEventType;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import tools.jackson.databind.json.JsonMapper;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TestPushHandlerTest {

    private static final UUID USER = UUID.randomUUID();

    private final PushDispatcher dispatcher = mock(PushDispatcher.class);
    private final TestPushHandler handler = new TestPushHandler(JsonMapper.builder().build(), dispatcher,
            messages(), new PushProperties(true, "", Locale.ENGLISH));

    @Test
    void dispatchesTheLocalizedTestMessageToTheUser() {
        handler.handle(new OutboxEvent(OutboxEventType.PUSH_TEST, "{\"userId\":\"" + USER + "\"}"));

        verify(dispatcher).dispatch(USER, PushMessage.of("Notifications are on", "It works.", Map.of("kind", "test")));
    }

    private static StaticMessageSource messages() {
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("push.test.title", Locale.ENGLISH, "Notifications are on");
        source.addMessage("push.test.body", Locale.ENGLISH, "It works.");
        return source;
    }
}
