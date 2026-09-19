package com.application.devhub.outbox;

public interface OutboxEventHandler {

    OutboxEventType type();

    void handle(OutboxEvent event);
}
