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

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatPushHandler implements OutboxEventHandler {

    private final JsonMapper jsonMapper;
    private final ChatQueries chatQueries;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final PushDispatcher pushDispatcher;
    private final ChatPushText pushText;

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
        memberRepository.findActive(conversation.getId()).stream()
                .filter(member -> !member.userId().equals(message.sender().id()))
                .forEach(member -> deliver(member.userId(), pushText.pushFor(payload.conversationId(), conversation,
                        message, conversation.getLastSeq() - member.getReadSeq())));
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
