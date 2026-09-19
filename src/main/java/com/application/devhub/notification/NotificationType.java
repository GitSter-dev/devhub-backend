package com.application.devhub.notification;

import java.util.UUID;

public enum NotificationType {
    POST_LIKED,
    POST_REPLIED,
    NEW_FOLLOWER,
    FOLLOWED_POSTED,
    MESSAGE_REQUEST;

    public String groupKey(UUID subjectId) {
        return subjectId == null ? name() : name() + ":" + subjectId;
    }
}
