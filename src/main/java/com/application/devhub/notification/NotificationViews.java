package com.application.devhub.notification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class NotificationViews {

    private NotificationViews() {
    }

    public record Actor(UUID id, String username, String displayName) {
    }

    public record NotificationView(UUID id, NotificationType type, List<Actor> actors, int actorCount, UUID subjectId,
                                   UUID targetId, String preview, boolean previewHasCode, Instant updatedAt,
                                   boolean seen) {
    }

    public record NotificationPage(List<NotificationView> items, String nextCursor) {
    }

    public record UnseenCount(long count) {
    }
}
