package com.application.devhub.realtime;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class StompTestSocket extends TextWebSocketHandler {

    private static final long TIMEOUT_SECONDS = 5;

    private final BlockingQueue<String> frames = new LinkedBlockingQueue<>();
    private final CompletableFuture<WebSocketSession> session = new CompletableFuture<>();
    private final CompletableFuture<CloseStatus> closed = new CompletableFuture<>();

    public static StompTestSocket connect(int port, String accessToken) throws Exception {
        StompTestSocket socket = new StompTestSocket();
        new StandardWebSocketClient().execute(socket, "ws://localhost:" + port + RealtimeConfig.ENDPOINT)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        String authorization = accessToken == null ? "" : "Authorization:Bearer " + accessToken + "\n";
        socket.send("CONNECT\naccept-version:1.2\nheart-beat:0,0\n" + authorization + "\n\0");
        return socket;
    }

    public void subscribe(String destination) throws Exception {
        send("SUBSCRIBE\nid:" + destination.hashCode() + "\ndestination:" + destination + "\n\n\0");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession established) {
        session.complete(established);
    }

    @Override
    protected void handleTextMessage(WebSocketSession ignored, TextMessage message) {
        frames.add(message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession ignored, CloseStatus status) {
        closed.complete(status);
    }

    public void send(String frame) throws Exception {
        session.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).sendMessage(new TextMessage(frame));
    }

    public String nextFrame() throws InterruptedException {
        return frames.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public String nextFrameWithin(long millis) throws InterruptedException {
        return frames.poll(millis, TimeUnit.MILLISECONDS);
    }

    public CloseStatus closeStatus() throws Exception {
        return closed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
