package com.application.devhub.chat;

import com.application.devhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TypingRelay {

    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ChatRealtimeRelay realtimeRelay;

    @Transactional(readOnly = true)
    public void relay(UUID typistId, UUID conversationId) {
        boolean active = memberRepository.find(conversationId, typistId).map(ConversationMember::isActive).orElse(false);
        if (!active) {
            return;
        }
        List<UUID> others = memberRepository.findActive(conversationId).stream()
                .map(ConversationMember::userId)
                .filter(userId -> !userId.equals(typistId))
                .toList();
        userRepository.findById(typistId).ifPresent(typist -> realtimeRelay.sendTo(others, new RealtimeEvent(
                RealtimeEvent.Type.TYPING, conversationId,
                Map.of("userId", typistId, "displayName", typist.getDisplayName()))));
    }
}
