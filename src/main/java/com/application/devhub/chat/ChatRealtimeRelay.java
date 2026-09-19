package com.application.devhub.chat;

import com.application.devhub.realtime.RealtimeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatRealtimeRelay {

    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationMemberRepository memberRepository;
    private final ChatQueries chatQueries;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onMessageCreated(ChatEvents.MessageCreated event) {
        RealtimeEvent realtime = new RealtimeEvent(RealtimeEvent.Type.MESSAGE_CREATED, event.conversationId(),
                chatQueries.message(event.messageId()));
        send(reachable(event.conversationId()), realtime);
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onMessageDeleted(ChatEvents.MessageDeleted event) {
        send(reachable(event.conversationId()), new RealtimeEvent(RealtimeEvent.Type.MESSAGE_DELETED,
                event.conversationId(), Map.of("messageId", event.messageId())));
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onReceiptUpdated(ChatEvents.ReceiptUpdated event) {
        boolean visible = memberRepository.find(event.conversationId(), event.userId())
                .map(ConversationMember::isActive)
                .orElse(false);
        if (visible) {
            send(active(event.conversationId()), new RealtimeEvent(RealtimeEvent.Type.RECEIPT_UPDATED,
                    event.conversationId(), Map.of("userId", event.userId(), "deliveredSeq", event.deliveredSeq(),
                    "readSeq", event.readSeq())));
        }
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onConversationChanged(ChatEvents.ConversationChanged event) {
        send(everyone(event.conversationId()), new RealtimeEvent(RealtimeEvent.Type.CONVERSATION_UPDATED,
                event.conversationId(), Map.of()));
    }

    public void sendTo(Collection<UUID> userIds, RealtimeEvent event) {
        send(userIds, event);
    }

    private Collection<UUID> reachable(UUID conversationId) {
        return memberRepository.findReachable(conversationId).stream().map(ConversationMember::userId).toList();
    }

    private Collection<UUID> active(UUID conversationId) {
        return memberRepository.findActive(conversationId).stream().map(ConversationMember::userId).toList();
    }

    private Collection<UUID> everyone(UUID conversationId) {
        return memberRepository.findAllMembers(conversationId).stream().map(ConversationMember::userId).toList();
    }

    private void send(Collection<UUID> userIds, RealtimeEvent event) {
        userIds.forEach(userId -> messagingTemplate.convertAndSendToUser(userId.toString(), RealtimeEvent.QUEUE, event));
    }
}
