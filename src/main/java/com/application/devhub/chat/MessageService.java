package com.application.devhub.chat;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.notification.ActivityPublisher;
import com.application.devhub.outbox.OutboxEventType;
import com.application.devhub.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final MessageLog messageLog;
    private final ReceiptService receiptService;
    private final ApplicationEventPublisher events;
    private final OutboxPublisher outboxPublisher;
    private final ActivityPublisher activityPublisher;

    @Transactional
    public UUID send(UUID senderId, UUID conversationId, SendMessageRequest request) {
        ConversationMember member = memberRepository.find(conversationId, senderId)
                .filter(ConversationMember::canReceive)
                .orElseThrow(ApiException::notFound);
        if (!member.isActive()) {
            throw ApiException.of(ErrorCode.CONVERSATION_REQUEST_PENDING);
        }
        var duplicate = messageRepository.findBySenderIdAndClientMessageId(senderId, request.clientMessageId());
        if (duplicate.isPresent()) {
            if (!duplicate.get().getConversationId().equals(conversationId)) {
                throw ApiException.of(ErrorCode.IDEMPOTENCY_KEY_REUSED);
            }
            return duplicate.get().getId();
        }
        if (request.replyToId() != null) {
            messageRepository.findById(request.replyToId())
                    .filter(original -> original.getConversationId().equals(conversationId))
                    .orElseThrow(ApiException::notFound);
        }
        Message message = messageLog.appendText(conversationId, senderId, request.content(), request.replyToId(),
                request.clientMessageId());
        receiptService.recordOwnMessage(senderId, conversationId, message.getSeq());
        events.publishEvent(new ChatEvents.MessageCreated(conversationId, message.getId()));
        outboxPublisher.publish(OutboxEventType.CHAT_MESSAGE,
                new ChatPushHandler.ChatMessagePayload(conversationId, message.getId()));
        if (message.getSeq() == 1 && hasPendingRequest(conversationId)) {
            activityPublisher.requestChanged(senderId, conversationId);
        }
        return message.getId();
    }

    private boolean hasPendingRequest(UUID conversationId) {
        return memberRepository.findAllMembers(conversationId).stream()
                .anyMatch(member -> member.getStatus() == MemberStatus.REQUEST);
    }

    @Transactional
    public void delete(UUID userId, UUID conversationId, UUID messageId) {
        memberRepository.find(conversationId, userId)
                .filter(ConversationMember::isActive)
                .orElseThrow(ApiException::notFound);
        Message message = messageRepository.findById(messageId)
                .filter(candidate -> candidate.getConversationId().equals(conversationId))
                .orElseThrow(ApiException::notFound);
        if (!message.isSentBy(userId)) {
            throw ApiException.forbidden();
        }
        if (!message.isDeleted()) {
            message.delete();
            events.publishEvent(new ChatEvents.MessageDeleted(conversationId, messageId));
        }
    }
}
