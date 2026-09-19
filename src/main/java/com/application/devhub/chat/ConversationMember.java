package com.application.devhub.chat;

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
@Table(name = "conversation_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConversationMember {

    @EmbeddedId
    private Key key;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MemberStatus status;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "delivered_seq", nullable = false)
    private long deliveredSeq;

    @Column(name = "read_seq", nullable = false)
    private long readSeq;

    private ConversationMember(UUID conversationId, UUID userId, MemberRole role, MemberStatus status) {
        this.key = new Key(conversationId, userId);
        this.role = role;
        this.status = status;
        this.joinedAt = Instant.now();
    }

    public static ConversationMember of(UUID conversationId, UUID userId, MemberRole role, MemberStatus status) {
        return new ConversationMember(conversationId, userId, role, status);
    }

    public UUID userId() {
        return key.userId();
    }

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    public boolean isOwner() {
        return role == MemberRole.OWNER;
    }

    public void activate() {
        if (status != MemberStatus.ACTIVE) {
            this.status = MemberStatus.ACTIVE;
            this.joinedAt = Instant.now();
        }
    }

    public void decline() {
        this.status = MemberStatus.DECLINED;
    }

    public void leave() {
        this.status = MemberStatus.LEFT;
        this.role = MemberRole.MEMBER;
    }

    public void makeOwner() {
        this.role = MemberRole.OWNER;
    }

    public boolean canReceive() {
        return status == MemberStatus.ACTIVE || status == MemberStatus.REQUEST;
    }

    public boolean advance(long delivered, long read, long lastSeq) {
        long nextRead = Math.min(lastSeq, Math.max(readSeq, read));
        long nextDelivered = Math.min(lastSeq, Math.max(deliveredSeq, Math.max(delivered, nextRead)));
        boolean changed = nextDelivered != deliveredSeq || nextRead != readSeq;
        this.deliveredSeq = nextDelivered;
        this.readSeq = nextRead;
        return changed;
    }

    public void catchUp(long lastSeq) {
        this.deliveredSeq = Math.max(deliveredSeq, lastSeq);
        this.readSeq = Math.max(readSeq, lastSeq);
    }

    @Embeddable
    public record Key(
            @Column(name = "conversation_id") UUID conversationId,
            @Column(name = "user_id") UUID userId) {
    }
}
