package com.application.devhub.notification;

import com.application.devhub.realtime.RealtimeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationRealtimeRelay {

    private static final RealtimeEvent CHANGED = new RealtimeEvent(RealtimeEvent.Type.NOTIFICATIONS_CHANGED, null, Map.of());

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener
    public void onNotificationsChanged(NotificationsChanged event) {
        event.recipientIds().forEach(userId ->
                messagingTemplate.convertAndSendToUser(userId.toString(), RealtimeEvent.QUEUE, CHANGED));
    }
}
