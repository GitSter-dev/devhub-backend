package com.application.devhub.block;

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
@Table(name = "blocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Block {

    @EmbeddedId
    private Key key;

    @Embeddable
    public record Key(
            @Column(name = "blocker_id") UUID blockerId,
            @Column(name = "blocked_id") UUID blockedId) {
    }
}
