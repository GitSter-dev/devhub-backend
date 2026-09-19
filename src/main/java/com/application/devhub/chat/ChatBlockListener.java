package com.application.devhub.chat;

import com.application.devhub.block.BlockChanged;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatBlockListener {

    private final ConversationRepository conversationRepository;
    private final ApplicationEventPublisher events;

    @EventListener
    public void onBlockChanged(BlockChanged event) {
        conversationRepository.findByDirectKey(Conversation.directKey(event.blockerId(), event.blockedId()))
                .ifPresent(direct -> events.publishEvent(new ChatEvents.ConversationChanged(direct.getId())));
    }
}
