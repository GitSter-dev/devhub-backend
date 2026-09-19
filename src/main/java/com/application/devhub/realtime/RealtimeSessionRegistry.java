package com.application.devhub.realtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Slf4j
@Component
public class RealtimeSessionRegistry {

    private final Map<String, Connection> connections = new ConcurrentHashMap<>();

    void opened(WebSocketSession socket) {
        connections.put(socket.getId(), Connection.anonymous(socket));
    }

    void closed(String socketId, CloseStatus status) {
        Connection connection = connections.remove(socketId);
        if (connection != null && connection.isIdentified()) {
            log.info("Realtime disconnected user={} session={} status={}", connection.userId(),
                    connection.sessionId(), status);
        }
    }

    boolean identify(String socketId, UUID userId, UUID sessionId) {
        return connections.computeIfPresent(socketId,
                (id, connection) -> connection.identifiedAs(userId, sessionId)) != null;
    }

    public void closeSession(UUID sessionId, CloseStatus status) {
        closeWhere(connection -> sessionId.equals(connection.sessionId()), status);
    }

    public void closeUser(UUID userId, CloseStatus status) {
        closeWhere(connection -> userId.equals(connection.userId()), status);
    }

    private void closeWhere(Predicate<Connection> matches, CloseStatus status) {
        connections.values().stream().filter(matches).forEach(connection -> close(connection.socket(), status));
    }

    private static void close(WebSocketSession socket, CloseStatus status) {
        try {
            socket.close(status);
        } catch (IOException e) {
            log.warn("Could not close realtime socket {}", socket.getId(), e);
        }
    }

    private record Connection(WebSocketSession socket, UUID userId, UUID sessionId) {

        static Connection anonymous(WebSocketSession socket) {
            return new Connection(socket, null, null);
        }

        Connection identifiedAs(UUID userId, UUID sessionId) {
            return new Connection(socket, userId, sessionId);
        }

        boolean isIdentified() {
            return userId != null;
        }
    }
}
