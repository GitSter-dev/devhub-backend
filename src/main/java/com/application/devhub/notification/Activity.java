package com.application.devhub.notification;

import java.util.UUID;

public record Activity(Kind kind, UUID actorId, UUID targetId, UUID cursor) {

    public enum Kind {
        LIKE,
        REPLY,
        FOLLOW,
        POST,
        MESSAGE_REQUEST
    }
}
