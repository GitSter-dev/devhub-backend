package com.application.devhub.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationPushJob {

    private final NotificationPusher pusher;

    @Scheduled(fixedDelayString = "${devhub.notifications.push-poll-interval}")
    void pushDueNotifications() {
        pusher.pushDue();
    }
}
