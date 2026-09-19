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
@Table(name = "messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends BaseEntity {

    @Column(name = "conversation_id", nullable = false, updatable = false)
    private UUID conversationId;

    @Column(nullable = false, updatable = false)
    private long seq;

    @Column(name = "sender_id", updatable = false)
    private UUID senderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private MessageKind kind;

    @Column(length = 4000)
    private String body;

    @Column(columnDefinition = "text")
    private String code;

    @Column(name = "code_language", length = 20)
    private String codeLanguage;

    @Column(name = "reply_to_id", updatable = false)
    private UUID replyToId;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_type", length = 20, updatable = false)
    private SystemEventType systemType;

    @Column(name = "system_actor_id", updatable = false)
    private UUID systemActorId;

    @Column(name = "system_target_id", updatable = false)
    private UUID systemTargetId;

    @Column(name = "client_message_id", updatable = false)
    private UUID clientMessageId;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "removed_at")
    private Instant removedAt;

    public static Message text(UUID conversationId, long seq, UUID senderId, MessageContent content, UUID replyToId,
                               UUID clientMessageId) {
        Message message = new Message();
        message.conversationId = conversationId;
        message.seq = seq;
        message.senderId = senderId;
        message.kind = MessageKind.TEXT;
        message.body = content.body();
        message.code = content.code();
        message.codeLanguage = content.codeLanguage();
        message.replyToId = replyToId;
        message.clientMessageId = clientMessageId;
        return message;
    }

    public static Message system(UUID conversationId, long seq, SystemEventType type, UUID actorId, UUID targetId,
                                 String detail) {
        Message message = new Message();
        message.conversationId = conversationId;
        message.seq = seq;
        message.kind = MessageKind.SYSTEM;
        message.systemType = type;
        message.systemActorId = actorId;
        message.systemTargetId = targetId;
        message.body = detail;
        return message;
    }

    public void remove() {
        this.removedAt = Instant.now();
        this.body = null;
        this.code = null;
        this.codeLanguage = null;
    }

    public void restore() {
        this.removedAt = null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isSentBy(UUID userId) {
        return userId.equals(senderId);
    }

    public void delete() {
        if (deletedAt == null) {
            this.deletedAt = Instant.now();
            this.body = null;
            this.code = null;
            this.codeLanguage = null;
        }
    }
}
