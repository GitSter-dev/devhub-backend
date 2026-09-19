package com.application.devhub.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "held_usernames")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HeldUsername {

    @Id
    @Column(name = "username_lower", length = 30)
    private String usernameLower;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "held_until", nullable = false)
    private Instant heldUntil;

    public boolean isHeldBy(UUID candidate) {
        return userId.equals(candidate);
    }
}
