package com.application.devhub.chat;

import com.application.devhub.common.persistence.BaseEntity;
import jakarta.persistence.Column;
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
@Table(name = "conversations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private ConversationKind kind;

    @Column(length = 80)
    private String title;

    @Column(name = "direct_key", length = 73, updatable = false)
    private String directKey;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "last_seq", nullable = false)
    private long lastSeq;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    private Conversation(ConversationKind kind, String title, String directKey, UUID createdBy) {
        this.kind = kind;
        this.title = title;
        this.directKey = directKey;
        this.createdBy = createdBy;
    }

    public static Conversation direct(UUID initiator, UUID other) {
        return new Conversation(ConversationKind.DIRECT, null, directKey(initiator, other), initiator);
    }

    public static Conversation group(UUID owner, String title) {
        return new Conversation(ConversationKind.GROUP, title, null, owner);
    }

    public static String directKey(UUID first, UUID second) {
        String a = first.toString();
        String b = second.toString();
        return a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a;
    }

    public boolean isGroup() {
        return kind == ConversationKind.GROUP;
    }

    public void rename(String title) {
        this.title = title;
    }

    public long nextSeq() {
        this.lastSeq += 1;
        this.lastMessageAt = Instant.now();
        return lastSeq;
    }
}
