package com.application.devhub.chat;

import com.application.devhub.chat.ChatViews.MessageView;
import com.application.devhub.outbox.OutboxEvent;
import com.application.devhub.outbox.OutboxEventHandler;
import com.application.devhub.outbox.OutboxEventType;
import com.application.devhub.push.PushDeliveryException;
import com.application.devhub.push.PushDispatcher;
import com.application.devhub.push.PushMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatPushHandler implements OutboxEventHandler {

    private static final String KIND = "chat";
    private static final int PREVIEW_LENGTH = 100;
    private static final String CODE_PREVIEW = "Sent a code snippet";

    private final JsonMapper jsonMapper;
    private final ChatQueries chatQueries;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final PushDispatcher pushDispatcher;

    @Override
    public OutboxEventType type() {
        return OutboxEventType.CHAT_MESSAGE;
    }

    @Override
    public void handle(OutboxEvent event) {
        ChatMessagePayload payload = jsonMapper.readValue(event.getPayload(), ChatMessagePayload.class);
        MessageView message = chatQueries.message(payload.messageId());
        if (message.deleted() || message.sender() == null) {
            return;
        }
        Conversation conversation = conversationRepository.findById(payload.conversationId()).orElse(null);
        if (conversation == null) {
            return;
        }
        PushMessage push = pushFor(payload.conversationId(), conversation, message);
        memberRepository.findActive(conversation.getId()).stream()
                .map(ConversationMember::userId)
                .filter(userId -> !userId.equals(message.sender().id()))
                .forEach(userId -> deliver(userId, push));
    }

    static PushMessage pushFor(UUID conversationId, Conversation conversation, MessageView message) {
        String preview = previewOf(message);
        String sender = message.sender().displayName();
        String title = conversation.isGroup() ? conversation.getTitle() : sender;
        String body = conversation.isGroup() ? sender + ": " + preview : preview;
        return new PushMessage(title, body, Map.of("kind", KIND, "conversationId", conversationId.toString(),
                "messageId", message.id().toString()));
    }

    private static String previewOf(MessageView message) {
        if (message.body() == null) {
            return CODE_PREVIEW;
        }
        return message.body().length() <= PREVIEW_LENGTH ? message.body() : message.body().substring(0, PREVIEW_LENGTH) + "…";
    }

    private void deliver(UUID userId, PushMessage push) {
        try {
            pushDispatcher.dispatch(userId, push);
        } catch (PushDeliveryException e) {
            log.warn("Chat push to {} failed; the message still syncs when the app opens", userId, e);
        }
    }

    public record ChatMessagePayload(UUID conversationId, UUID messageId) {
    }
}
