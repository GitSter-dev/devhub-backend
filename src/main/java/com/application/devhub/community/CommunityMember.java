package com.application.devhub.community;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "community_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityMember {

    @EmbeddedId
    private Key key;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "last_posted_at")
    private Instant lastPostedAt;

    private CommunityMember(Key key, CommunityRole role) {
        this.key = key;
        this.role = role;
        this.joinedAt = Instant.now();
    }

    public static CommunityMember owner(UUID communityId, UUID userId) {
        return new CommunityMember(new Key(communityId, userId), CommunityRole.OWNER);
    }

    public static CommunityMember member(UUID communityId, UUID userId) {
        return new CommunityMember(new Key(communityId, userId), CommunityRole.MEMBER);
    }

    public boolean canModerate() {
        return role.canModerate();
    }

    public boolean isOwner() {
        return role == CommunityRole.OWNER;
    }

    public void promote() {
        if (role == CommunityRole.MEMBER) {
            this.role = CommunityRole.MODERATOR;
        }
    }

    public void demote() {
        if (role == CommunityRole.MODERATOR) {
            this.role = CommunityRole.MEMBER;
        }
    }

    public void stepDown() {
        if (role == CommunityRole.OWNER) {
            this.role = CommunityRole.MODERATOR;
        }
    }

    public void becomeOwner() {
        this.role = CommunityRole.OWNER;
    }

    public UUID userId() {
        return key.userId();
    }

    public void posted() {
        this.lastPostedAt = Instant.now();
    }

    @Embeddable
    public record Key(
            @Column(name = "community_id") UUID communityId,
            @Column(name = "user_id") UUID userId) {
    }
}
