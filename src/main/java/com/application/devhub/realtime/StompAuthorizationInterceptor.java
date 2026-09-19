package com.application.devhub.realtime;

import com.application.devhub.common.api.ErrorCode;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class StompAuthorizationInterceptor implements ChannelInterceptor {

    private static final String USER_QUEUES = "/user/queue/";
    private static final Set<StompCommand> UNAUTHENTICATED_COMMANDS =
            EnumSet.of(StompCommand.CONNECT, StompCommand.STOMP, StompCommand.DISCONNECT);

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null
                || UNAUTHENTICATED_COMMANDS.contains(accessor.getCommand())) {
            return message;
        }
        if (accessor.getUser() == null) {
            throw StompRejection.of(ErrorCode.UNAUTHORIZED);
        }
        if (accessor.getCommand() == StompCommand.SEND) {
            throw StompRejection.of(ErrorCode.FORBIDDEN);
        }
        if (accessor.getCommand() == StompCommand.SUBSCRIBE && !isUserQueue(accessor.getDestination())) {
            throw StompRejection.of(ErrorCode.FORBIDDEN);
        }
        return message;
    }

    private static boolean isUserQueue(String destination) {
        return destination != null && destination.startsWith(USER_QUEUES);
    }
}
