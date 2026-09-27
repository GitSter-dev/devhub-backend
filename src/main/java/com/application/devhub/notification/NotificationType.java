package com.application.devhub.notification;

import com.application.devhub.client.ClientVersion;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public enum NotificationType {
    POST_LIKED("1.0.0"),
    POST_REPLIED("1.0.0"),
    NEW_FOLLOWER("1.0.0"),
    FOLLOWED_POSTED("1.0.0"),
    MESSAGE_REQUEST("1.0.0"),
    REPORT_RESOLVED("1.1.0"),
    COMMUNITY_JOIN_REQUESTED("1.1.0"),
    COMMUNITY_JOIN_APPROVED("1.1.0"),
    COMMUNITY_POST_REMOVED("1.1.0");

    private final ClientVersion shownFrom;

    NotificationType(String shownFrom) {
        this.shownFrom = ClientVersion.parse(shownFrom).orElseThrow();
    }

    public String groupKey(UUID subjectId) {
        return subjectId == null ? name() : name() + ":" + subjectId;
    }

    public static Set<NotificationType> shownTo(ClientVersion appVersion) {
        return Arrays.stream(values())
                .filter(type -> !appVersion.isOlderThan(type.shownFrom))
                .collect(Collectors.toUnmodifiableSet());
    }
}
