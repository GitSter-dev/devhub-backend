package com.application.devhub.follow;

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
@Table(name = "follows")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow {

    @EmbeddedId
    private Key key;

    @Embeddable
    public record Key(
            @Column(name = "follower_id") UUID followerId,
            @Column(name = "followee_id") UUID followeeId) {
    }
}
