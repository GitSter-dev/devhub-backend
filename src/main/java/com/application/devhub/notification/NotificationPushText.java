package com.application.devhub.notification;

import com.application.devhub.notification.NotificationViews.Actor;
import com.application.devhub.notification.NotificationViews.NotificationView;
import com.application.devhub.push.PushMessage;
import com.application.devhub.push.PushProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationPushText {

    private static final String KIND = "activity";

    private final MessageSource messageSource;
    private final PushProperties properties;

    public PushMessage pushFor(NotificationView notification) {
        String title = text("notification.title." + notification.type().name(), who(notification));
        Map<String, String> data = new HashMap<>();
        data.put("kind", KIND);
        data.put("notificationId", notification.id().toString());
        data.put("type", notification.type().name());
        if (notification.subjectId() != null) {
            data.put("subjectId", notification.subjectId().toString());
        }
        if (notification.targetId() != null) {
            data.put("targetId", notification.targetId().toString());
        }
        data.put("actorCount", Integer.toString(notification.actorCount()));
        data.put("actorUsername", notification.actors().getFirst().username());
        return PushMessage.grouped(title, body(notification), data, groupOf(notification));
    }

    private String who(NotificationView notification) {
        List<String> names = notification.actors().stream().map(Actor::displayName).toList();
        int count = notification.actorCount();
        if (count <= 1 || names.size() < 2) {
            return text("notification.who.one", names.getFirst());
        }
        if (count == 2) {
            return text("notification.who.two", names.get(0), names.get(1));
        }
        return text("notification.who.many", names.get(0), names.get(1), count - 2);
    }

    private String body(NotificationView notification) {
        if (notification.preview() != null) {
            return notification.preview();
        }
        return notification.previewHasCode() ? text("notification.code") : null;
    }

    private static String groupOf(NotificationView notification) {
        String subject = notification.subjectId() == null ? "" : ":" + notification.subjectId();
        return KIND + ":" + notification.type().name() + subject;
    }

    private String text(String key, Object... arguments) {
        return messageSource.getMessage(key, arguments, properties.locale());
    }
}
