package com.application.devhub.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository repository;
    private final JsonMapper jsonMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(OutboxEventType type, Object payload) {
        repository.save(new OutboxEvent(type, jsonMapper.writeValueAsString(payload)));
    }
}
