package com.application.devhub.chat;

import java.util.UUID;

public record RealtimeEvent(Type type, UUID conversationId, Object data) {

    public enum Type {
        MESSAGE_CREATED,
        MESSAGE_DELETED,
        RECEIPT_UPDATED,
        CONVERSATION_UPDATED,
        TYPING
    }
}
