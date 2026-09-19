package com.application.devhub.chat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ChatViews {

    private ChatViews() {
    }

    public record PersonRef(UUID id, String username, String displayName) {
    }

    public record MemberView(UUID id, String username, String displayName, MemberRole role, MemberStatus status,
                             Long deliveredSeq, Long readSeq) {
    }

    public record ReplyPreview(UUID id, long seq, String senderName, String preview, boolean deleted) {
    }

    public record SystemView(SystemEventType type, PersonRef actor, PersonRef target, String detail) {
    }

    public record MessageView(UUID id, long seq, PersonRef sender, MessageKind kind, String body, String code,
                              String codeLanguage, Instant createdAt, boolean deleted, UUID clientMessageId,
                              ReplyPreview replyTo, SystemView system) {
    }

    public record ConversationView(UUID id, ConversationKind kind, String title, MemberStatus myStatus, long lastSeq,
                                   long myReadSeq, long unreadCount, long othersDeliveredSeq, long othersReadSeq,
                                   Instant lastActivityAt, List<MemberView> members, MessageView lastMessage) {
    }

    public record ConversationPage(List<ConversationView> items, String nextCursor) {
    }

    public record MessagePage(List<MessageView> items, boolean hasMore) {
    }
}
