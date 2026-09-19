package com.application.devhub.realtime;

import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.security.Principal;

final class StompFrames {

    static final String SOCKET_ID = "socket-1";

    private StompFrames() {
    }

    static Message<byte[]> connect(String authorization) {
        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT, null);
        if (authorization != null) {
            accessor.addNativeHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        return build(accessor);
    }

    static Message<byte[]> frame(StompCommand command, String destination, Principal user) {
        StompHeaderAccessor accessor = accessor(command, destination);
        accessor.setUser(user);
        return build(accessor);
    }

    static Message<byte[]> heartbeat() {
        StompHeaderAccessor accessor = StompHeaderAccessor.createForHeartbeat();
        accessor.setSessionId(SOCKET_ID);
        accessor.setLeaveMutable(true);
        return build(accessor);
    }

    static StompHeaderAccessor headersOf(Message<?> message) {
        return StompHeaderAccessor.wrap(message);
    }

    private static StompHeaderAccessor accessor(StompCommand command, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId(SOCKET_ID);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        accessor.setLeaveMutable(true);
        return accessor;
    }

    private static Message<byte[]> build(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
