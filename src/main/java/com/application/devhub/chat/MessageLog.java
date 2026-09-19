package com.application.devhub.chat;

import com.application.devhub.common.api.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MessageLog {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public Message appendText(UUID conversationId, UUID senderId, MessageContent content, UUID replyToId,
                              UUID clientMessageId) {
        long seq = lock(conversationId).nextSeq();
        return messageRepository.save(Message.text(conversationId, seq, senderId, content, replyToId, clientMessageId));
    }

    public Message appendSystem(UUID conversationId, SystemEventType type, UUID actorId, UUID targetId, String detail) {
        long seq = lock(conversationId).nextSeq();
        return messageRepository.save(Message.system(conversationId, seq, type, actorId, targetId, detail));
    }

    private Conversation lock(UUID conversationId) {
        return conversationRepository.findByIdForUpdate(conversationId).orElseThrow(ApiException::notFound);
    }
}
