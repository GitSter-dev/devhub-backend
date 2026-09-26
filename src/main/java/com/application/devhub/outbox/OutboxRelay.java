package com.application.devhub.outbox;

import com.application.devhub.common.metrics.DevHubMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OutboxRelay {

    private final OutboxEventRepository repository;
    private final OutboxProperties properties;
    private final DevHubMetrics metrics;
    private final Map<OutboxEventType, OutboxEventHandler> handlers = new EnumMap<>(OutboxEventType.class);

    public OutboxRelay(OutboxEventRepository repository, OutboxProperties properties,
                       List<OutboxEventHandler> handlers, DevHubMetrics metrics) {
        this.repository = repository;
        this.properties = properties;
        this.metrics = metrics;
        handlers.forEach(handler -> this.handlers.put(handler.type(), handler));
    }

    @Transactional
    public void relayPending() {
        repository.claimDue(properties.batchSize()).forEach(this::dispatch);
    }

    private void dispatch(OutboxEvent event) {
        OutboxEventHandler handler = handlers.get(event.getType());
        try {
            if (handler == null) {
                throw new IllegalStateException("No handler for outbox event type " + event.getType());
            }
            handler.handle(event);
            event.markSent();
            metrics.outboxEvent(event.getType(), "sent");
        } catch (RuntimeException e) {
            log.warn("Outbox event {} of type {} failed (attempt {})", event.getId(), event.getType(),
                    event.getAttempts() + 1, e);
            event.recordFailure(e.getMessage(), properties.maxAttempts(), properties.retryBackoff());
            metrics.outboxEvent(event.getType(), event.getStatus() == OutboxStatus.FAILED ? "failed" : "retrying");
        }
    }
}
