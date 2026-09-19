package com.application.devhub.chat;

import com.application.devhub.common.api.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final ApplicationEventPublisher events;

    @Transactional
    public Watermarks advance(UUID userId, UUID conversationId, long delivered, long read) {
        ConversationMember member = memberRepository.findForUpdate(conversationId, userId)
                .filter(ConversationMember::canReceive)
                .orElseThrow(ApiException::notFound);
        long lastSeq = conversationRepository.findById(conversationId).orElseThrow(ApiException::notFound).getLastSeq();
        if (member.advance(delivered, read, lastSeq)) {
            events.publishEvent(new ChatEvents.ReceiptUpdated(conversationId, userId, member.getDeliveredSeq(),
                    member.getReadSeq()));
        }
        return new Watermarks(member.getDeliveredSeq(), member.getReadSeq());
    }

    @Transactional
    public void catchUp(UUID conversationId, Collection<UUID> userIds, long lastSeq) {
        userIds.forEach(userId -> memberRepository.find(conversationId, userId).ifPresent(member -> member.catchUp(lastSeq)));
    }

    public record Watermarks(long deliveredSeq, long readSeq) {
    }
}
