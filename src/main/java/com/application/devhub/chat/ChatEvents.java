package com.application.devhub.chat;

import java.util.UUID;

public final class ChatEvents {

    private ChatEvents() {
    }

    public record MessageCreated(UUID conversationId, UUID messageId) {
    }

    public record MessageDeleted(UUID conversationId, UUID messageId) {
    }

    public record ReceiptUpdated(UUID conversationId, UUID userId, long deliveredSeq, long readSeq) {
    }

    public record ConversationChanged(UUID conversationId) {
    }
}
