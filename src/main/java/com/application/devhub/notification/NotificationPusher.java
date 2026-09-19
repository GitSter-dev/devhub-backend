package com.application.devhub.notification;

import com.application.devhub.notification.NotificationViews.NotificationView;
import com.application.devhub.push.PushDeliveryException;
import com.application.devhub.push.PushDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPusher {

    private final NotificationRepository repository;
    private final NotificationQueries queries;
    private final NotificationPushText pushText;
    private final PushDispatcher pushDispatcher;
    private final NotificationProperties properties;

    @Transactional
    public int pushDue() {
        List<Notification> due = repository.claimDue(properties.pushBatchSize());
        due.forEach(this::push);
        return due.size();
    }

    private void push(Notification notification) {
        Optional<NotificationView> view = queries.find(notification.getId());
        if (view.isEmpty() || notification.getSeenAt() != null) {
            notification.skipPush();
            return;
        }
        try {
            pushDispatcher.dispatch(notification.getRecipientId(), pushText.pushFor(view.get()));
        } catch (PushDeliveryException e) {
            log.warn("Push for notification {} failed; it stays in the list", notification.getId(), e);
        }
        notification.markPushed();
    }
}
