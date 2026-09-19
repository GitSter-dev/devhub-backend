package com.application.devhub.chat;

import com.application.devhub.chat.ChatViews.ConversationPage;
import com.application.devhub.chat.ChatViews.ConversationView;
import com.application.devhub.chat.ChatViews.MemberView;
import com.application.devhub.chat.ChatViews.MessagePage;
import com.application.devhub.chat.ChatViews.MessageView;
import com.application.devhub.chat.ChatViews.PersonRef;
import com.application.devhub.chat.ChatViews.ReplyPreview;
import com.application.devhub.chat.ChatViews.SystemView;
import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.pagination.KeysetCursor;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChatQueries {

    public static final int CONVERSATION_PAGE_SIZE = 20;
    public static final int MESSAGE_PAGE_SIZE = 30;
    public static final int MAX_MESSAGE_PAGE_SIZE = 100;
    private static final int REQUESTS_LIMIT = 50;

    private static final String CONVERSATIONS = """
            SELECT c.id, c.kind, c.title, c.last_seq, COALESCE(c.last_message_at, c.created_at) AS activity_at,
                   me.status AS my_status, me.read_seq AS my_read_seq
            FROM conversations c
            JOIN conversation_members me ON me.conversation_id = c.id AND me.user_id = :viewer
            """;
    private static final String MEMBERS = """
            SELECT cm.conversation_id, u.id, u.username, u.display_name, cm.role, cm.status, cm.delivered_seq, cm.read_seq
            FROM conversation_members cm
            JOIN users u ON u.id = cm.user_id
            WHERE cm.conversation_id IN (:conversationIds) AND cm.status IN ('ACTIVE', 'REQUEST')
            ORDER BY cm.joined_at, u.id
            """;
    private static final String LAST_MESSAGES = """
            SELECT DISTINCT ON (conversation_id) id FROM messages
            WHERE conversation_id IN (:conversationIds)
            ORDER BY conversation_id, seq DESC
            """;
    private static final String MESSAGES = """
            SELECT m.id, m.conversation_id, m.seq, m.kind, m.body, m.code, m.code_language, m.created_at,
                   m.deleted_at IS NOT NULL AS deleted, m.client_message_id,
                   s.id AS sender_id, s.username AS sender_username, s.display_name AS sender_display_name,
                   r.id AS reply_id, r.seq AS reply_seq, r.deleted_at IS NOT NULL AS reply_deleted,
                   rs.display_name AS reply_sender_name,
                   left(COALESCE(r.body, CASE WHEN r.code IS NOT NULL THEN 'Code snippet' END), 120) AS reply_preview,
                   m.system_type,
                   sa.id AS actor_id, sa.username AS actor_username, sa.display_name AS actor_display_name,
                   st.id AS target_id, st.username AS target_username, st.display_name AS target_display_name
            FROM messages m
            LEFT JOIN users s ON s.id = m.sender_id
            LEFT JOIN messages r ON r.id = m.reply_to_id
            LEFT JOIN users rs ON rs.id = r.sender_id
            LEFT JOIN users sa ON sa.id = m.system_actor_id
            LEFT JOIN users st ON st.id = m.system_target_id
            """;

    private final JdbcClient jdbcClient;

    @Transactional(readOnly = true)
    public ConversationPage inbox(UUID viewerId, String cursor) {
        KeysetCursor position = KeysetCursor.decode(cursor);
        String keyset = position == null ? ""
                : " AND (COALESCE(c.last_message_at, c.created_at), c.id) < (CAST(:cursorAt AS timestamptz), CAST(:cursorId AS uuid))";
        var query = jdbcClient.sql(CONVERSATIONS + " WHERE me.status = 'ACTIVE' AND c.last_seq > 0" + keyset
                        + " ORDER BY activity_at DESC, c.id DESC LIMIT :limit")
                .param("viewer", viewerId)
                .param("limit", CONVERSATION_PAGE_SIZE + 1);
        if (position != null) {
            query = query.param("cursorAt", position.timestamp()).param("cursorId", position.id());
        }
        List<ConversationRow> rows = query.query((row, index) -> ConversationRow.of(row)).list();
        boolean more = rows.size() > CONVERSATION_PAGE_SIZE;
        List<ConversationRow> page = more ? rows.subList(0, CONVERSATION_PAGE_SIZE) : rows;
        String next = more ? new KeysetCursor(page.getLast().activityAt(), page.getLast().id()).encode() : null;
        return new ConversationPage(assemble(viewerId, page), next);
    }

    @Transactional(readOnly = true)
    public List<ConversationView> requests(UUID viewerId) {
        List<ConversationRow> rows = jdbcClient.sql(CONVERSATIONS + " WHERE me.status = 'REQUEST' AND c.last_seq > 0"
                        + " ORDER BY activity_at DESC, c.id DESC LIMIT :limit")
                .param("viewer", viewerId)
                .param("limit", REQUESTS_LIMIT)
                .query((row, index) -> ConversationRow.of(row))
                .list();
        return assemble(viewerId, rows);
    }

    @Transactional(readOnly = true)
    public ConversationView conversation(UUID viewerId, UUID conversationId) {
        ConversationRow row = jdbcClient.sql(CONVERSATIONS + " WHERE c.id = :conversationId AND me.status IN ('ACTIVE', 'REQUEST')")
                .param("viewer", viewerId)
                .param("conversationId", conversationId)
                .query((result, index) -> ConversationRow.of(result))
                .optional()
                .orElseThrow(ApiException::notFound);
        return assemble(viewerId, List.of(row)).getFirst();
    }

    @Transactional(readOnly = true)
    public MessageView message(UUID messageId) {
        return messages(List.of(messageId)).stream().findFirst().orElseThrow(ApiException::notFound);
    }

    @Transactional(readOnly = true)
    public MessagePage messages(UUID viewerId, UUID conversationId, Long beforeSeq, Long afterSeq, int limit) {
        conversation(viewerId, conversationId);
        int size = Math.clamp(limit, 1, MAX_MESSAGE_PAGE_SIZE);
        boolean forward = afterSeq != null;
        String where = forward ? " WHERE m.conversation_id = :conversationId AND m.seq > :afterSeq ORDER BY m.seq ASC"
                : " WHERE m.conversation_id = :conversationId AND m.seq < :beforeSeq ORDER BY m.seq DESC";
        List<MessageView> rows = jdbcClient.sql(MESSAGES + where + " LIMIT :limit")
                .param("conversationId", conversationId)
                .param(forward ? "afterSeq" : "beforeSeq", forward ? afterSeq : (beforeSeq == null ? Long.MAX_VALUE : beforeSeq))
                .param("limit", size + 1)
                .query((row, index) -> toMessage(row))
                .list();
        boolean more = rows.size() > size;
        List<MessageView> page = new ArrayList<>(more ? rows.subList(0, size) : rows);
        if (!forward) {
            Collections.reverse(page);
        }
        return new MessagePage(page, more);
    }

    private List<ConversationView> assemble(UUID viewerId, List<ConversationRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = rows.stream().map(ConversationRow::id).toList();
        Map<UUID, List<MemberRow>> members = jdbcClient.sql(MEMBERS)
                .param("conversationIds", ids)
                .query((row, index) -> MemberRow.of(row))
                .list().stream()
                .collect(Collectors.groupingBy(MemberRow::conversationId));
        List<UUID> lastIds = jdbcClient.sql(LAST_MESSAGES).param("conversationIds", ids).query(UUID.class).list();
        Map<UUID, MessageView> lastMessages = new HashMap<>();
        messagesWithConversation(lastIds).forEach(entry -> lastMessages.put(entry.conversationId(), entry.view()));
        return rows.stream()
                .map(row -> row.toView(viewerId, members.getOrDefault(row.id(), List.of()), lastMessages.get(row.id())))
                .toList();
    }

    private List<MessageView> messages(List<UUID> messageIds) {
        return messagesWithConversation(messageIds).stream().map(ConversationMessage::view).toList();
    }

    private List<ConversationMessage> messagesWithConversation(List<UUID> messageIds) {
        if (messageIds.isEmpty()) {
            return List.of();
        }
        return jdbcClient.sql(MESSAGES + " WHERE m.id IN (:messageIds)")
                .param("messageIds", messageIds)
                .query((row, index) -> new ConversationMessage(row.getObject("conversation_id", UUID.class), toMessage(row)))
                .list();
    }

    private static MessageView toMessage(ResultSet row) throws SQLException {
        UUID replyId = row.getObject("reply_id", UUID.class);
        String systemType = row.getString("system_type");
        return new MessageView(
                row.getObject("id", UUID.class),
                row.getLong("seq"),
                person(row, "sender"),
                MessageKind.valueOf(row.getString("kind")),
                row.getString("body"),
                row.getString("code"),
                row.getString("code_language"),
                row.getTimestamp("created_at").toInstant(),
                row.getBoolean("deleted"),
                row.getObject("client_message_id", UUID.class),
                replyId == null ? null : new ReplyPreview(replyId, row.getLong("reply_seq"), row.getString("reply_sender_name"),
                        row.getBoolean("reply_deleted") ? null : row.getString("reply_preview"), row.getBoolean("reply_deleted")),
                systemType == null ? null : new SystemView(SystemEventType.valueOf(systemType), person(row, "actor"),
                        person(row, "target"), row.getString("body")));
    }

    private static PersonRef person(ResultSet row, String prefix) throws SQLException {
        UUID id = row.getObject(prefix + "_id", UUID.class);
        return id == null ? null : new PersonRef(id, row.getString(prefix + "_username"), row.getString(prefix + "_display_name"));
    }

    private record ConversationMessage(UUID conversationId, MessageView view) {
    }

    private record MemberRow(UUID conversationId, UUID userId, String username, String displayName, MemberRole role,
                             MemberStatus status, long deliveredSeq, long readSeq) {

        static MemberRow of(ResultSet row) throws SQLException {
            return new MemberRow(row.getObject("conversation_id", UUID.class), row.getObject("id", UUID.class),
                    row.getString("username"), row.getString("display_name"), MemberRole.valueOf(row.getString("role")),
                    MemberStatus.valueOf(row.getString("status")), row.getLong("delivered_seq"), row.getLong("read_seq"));
        }

        MemberView toView(UUID viewerId) {
            boolean hidden = status == MemberStatus.REQUEST && !userId.equals(viewerId);
            return new MemberView(userId, username, displayName, role, status, hidden ? null : deliveredSeq,
                    hidden ? null : readSeq);
        }

        long visibleDelivered() {
            return status == MemberStatus.ACTIVE ? deliveredSeq : 0;
        }

        long visibleRead() {
            return status == MemberStatus.ACTIVE ? readSeq : 0;
        }
    }

    private record ConversationRow(UUID id, ConversationKind kind, String title, long lastSeq, Instant activityAt,
                                   MemberStatus myStatus, long myReadSeq) {

        static ConversationRow of(ResultSet row) throws SQLException {
            return new ConversationRow(row.getObject("id", UUID.class), ConversationKind.valueOf(row.getString("kind")),
                    row.getString("title"), row.getLong("last_seq"), row.getTimestamp("activity_at").toInstant(),
                    MemberStatus.valueOf(row.getString("my_status")), row.getLong("my_read_seq"));
        }

        ConversationView toView(UUID viewerId, List<MemberRow> members, MessageView lastMessage) {
            List<MemberRow> others = members.stream().filter(member -> !member.userId().equals(viewerId)).toList();
            long othersDelivered = others.stream().mapToLong(MemberRow::visibleDelivered).min().orElse(0);
            long othersRead = others.stream().mapToLong(MemberRow::visibleRead).min().orElse(0);
            return new ConversationView(id, kind, title, myStatus, lastSeq, myReadSeq, Math.max(0, lastSeq - myReadSeq),
                    othersDelivered, othersRead, activityAt,
                    members.stream().map(member -> member.toView(viewerId)).toList(), lastMessage);
        }
    }
}
