package com.application.devhub.realtime;

import java.util.UUID;

public record RealtimeEvent(Type type, UUID conversationId, Object data) {

    public static final String QUEUE = "/queue/events";

    public enum Type {
        MESSAGE_CREATED,
        MESSAGE_DELETED,
        RECEIPT_UPDATED,
        CONVERSATION_UPDATED,
        TYPING,
        NOTIFICATIONS_CHANGED
    }
}
