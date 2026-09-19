package com.application.devhub.realtime;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

class RealtimeSessionTracking extends WebSocketHandlerDecorator {

    private final RealtimeSessionRegistry registry;

    RealtimeSessionTracking(WebSocketHandler delegate, RealtimeSessionRegistry registry) {
        super(delegate);
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        registry.opened(session);
        super.afterConnectionEstablished(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        registry.closed(session.getId(), closeStatus);
        super.afterConnectionClosed(session, closeStatus);
    }
}
