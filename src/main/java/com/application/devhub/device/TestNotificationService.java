package com.application.devhub.device;

import com.application.devhub.outbox.OutboxEventType;
import com.application.devhub.outbox.OutboxPublisher;
import com.application.devhub.user.UserEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TestNotificationService {

    private final OutboxPublisher outboxPublisher;

    @Transactional
    public void request(UUID userId) {
        outboxPublisher.publish(OutboxEventType.PUSH_TEST, new UserEventPayload(userId));
    }
}
