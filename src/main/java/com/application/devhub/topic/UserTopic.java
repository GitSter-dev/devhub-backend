package com.application.devhub.topic;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "user_topics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTopic {

    @EmbeddedId
    private Key key;

    private UserTopic(Key key) {
        this.key = key;
    }

    public static UserTopic of(UUID userId, String topicSlug) {
        return new UserTopic(new Key(userId, topicSlug));
    }

    @Embeddable
    public record Key(
            @Column(name = "user_id") UUID userId,
            @Column(name = "topic_slug", length = 40) String topicSlug) {
    }
}
