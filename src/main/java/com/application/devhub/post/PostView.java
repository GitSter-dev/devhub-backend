package com.application.devhub.post;

import java.time.Instant;
import java.util.UUID;

public record PostView(
        UUID id,
        Author author,
        String body,
        String code,
        String codeLanguage,
        Instant createdAt,
        UUID replyToId,
        String replyToUsername,
        boolean replyToDeleted,
        UUID rootId,
        long replyCount,
        long likeCount,
        boolean liked,
        boolean mine,
        boolean deleted,
        boolean removed,
        boolean underReview) {

    public record Author(UUID id, String username, String displayName) {
    }
}
