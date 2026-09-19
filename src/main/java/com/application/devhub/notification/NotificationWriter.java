package com.application.devhub.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationWriter {

    private final NotificationRepository repository;
    private final NotificationProperties properties;
    private final ApplicationEventPublisher events;

    @Transactional
    public void record(Collection<UUID> recipientIds, NotificationType type, UUID subjectId, UUID actorId,
                       UUID actorSubjectId) {
        if (recipientIds.isEmpty()) {
            return;
        }
        repository.record(recipientIds, type.name(), type.groupKey(subjectId), subjectId, actorId, actorSubjectId,
                seconds(properties.burstWindow()), seconds(properties.digestFor(type)), properties.individualPushes());
        events.publishEvent(new NotificationsChanged(recipientIds));
    }

    @Transactional
    public void withdraw(UUID recipientId, NotificationType type, UUID subjectId, UUID actorId) {
        if (repository.withdraw(recipientId, type.groupKey(subjectId), actorId) > 0) {
            events.publishEvent(new NotificationsChanged(List.of(recipientId)));
        }
    }

    @Transactional
    public void withdrawBetween(UUID first, UUID second) {
        if (repository.withdrawBetween(first, second) > 0) {
            events.publishEvent(new NotificationsChanged(List.of(first, second)));
        }
    }

    @Transactional
    public void withdrawSubject(UUID subjectId) {
        List<UUID> recipientIds = repository.recipientsOfSubject(subjectId);
        if (!recipientIds.isEmpty()) {
            repository.withdrawSubject(subjectId);
            events.publishEvent(new NotificationsChanged(recipientIds));
        }
    }

    @Transactional
    public void markSeen(UUID recipientId, Instant until) {
        repository.markSeen(recipientId, until);
    }

    private static double seconds(Duration duration) {
        return duration.toMillis() / 1000.0;
    }
}
