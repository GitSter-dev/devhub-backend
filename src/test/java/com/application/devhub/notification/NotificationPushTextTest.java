package com.application.devhub.notification;

import com.application.devhub.notification.NotificationViews.Actor;
import com.application.devhub.notification.NotificationViews.NotificationView;
import com.application.devhub.push.PushMessage;
import com.application.devhub.push.PushProperties;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationPushTextTest {

    private static final Actor ADA = new Actor(UUID.randomUUID(), "ada", "Ada");
    private static final Actor KEN = new Actor(UUID.randomUUID(), "ken", "Ken");
    private static final Actor LINUS = new Actor(UUID.randomUUID(), "linus", "Linus");

    private final NotificationPushText pushText = new NotificationPushText(messages(), new PushProperties(false, null, Locale.ENGLISH));

    @Test
    void namesUpToTwoPeopleThenCountsTheRest() {
        assertThat(title(NotificationType.NEW_FOLLOWER, 1, ADA)).isEqualTo("Ada followed you");
        assertThat(title(NotificationType.POST_LIKED, 2, ADA, KEN)).isEqualTo("Ada and Ken liked your post");
        assertThat(title(NotificationType.POST_REPLIED, 3, ADA, KEN, LINUS)).isEqualTo("Ada, Ken and 1 other replied to your post");
        assertThat(title(NotificationType.FOLLOWED_POSTED, 12, ADA, KEN, LINUS)).isEqualTo("Ada, Ken and 10 others posted");
        assertThat(title(NotificationType.MESSAGE_REQUEST, 1, KEN)).isEqualTo("Ken wants to message you");
    }

    @Test
    void theBodyIsThePreviewOrACodeHint() {
        assertThat(pushText.pushFor(view(NotificationType.POST_LIKED, 1, "hello", false, ADA)).body()).isEqualTo("hello");
        assertThat(pushText.pushFor(view(NotificationType.POST_LIKED, 1, null, true, ADA)).body()).isEqualTo("Shared a code snippet");
        assertThat(pushText.pushFor(view(NotificationType.NEW_FOLLOWER, 1, null, false, ADA)).body()).isNull();
    }

    private String title(NotificationType type, int count, Actor... actors) {
        return pushText.pushFor(view(type, count, null, false, actors)).title();
    }

    private static NotificationView view(NotificationType type, int count, String preview, boolean code, Actor... actors) {
        return new NotificationView(UUID.randomUUID(), type, List.of(actors), count, UUID.randomUUID(), UUID.randomUUID(),
                preview, code, Instant.now(), false);
    }

    private static ResourceBundleMessageSource messages() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
