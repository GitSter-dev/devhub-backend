package com.application.devhub.chat;

import com.application.devhub.chat.ChatViews.MessageView;
import com.application.devhub.chat.ChatViews.PersonRef;
import com.application.devhub.push.PushMessage;
import com.application.devhub.push.PushProperties;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ChatPushTextTest {

    private static final PersonRef ADA = new PersonRef(UUID.randomUUID(), "ada", "Ada");

    private final ChatPushText pushText = new ChatPushText(messages(), new PushProperties(false, null, Locale.ENGLISH));

    @Test
    void aDirectMessageIsTitledWithTheSender() {
        PushMessage push = pushText.pushFor(UUID.randomUUID(), Conversation.direct(ADA.id(), UUID.randomUUID()), message("hey there", null), 1);

        assertThat(push.title()).isEqualTo("Ada");
        assertThat(push.body()).isEqualTo("hey there");
        assertThat(push.data()).containsEntry("kind", "chat");
    }

    @Test
    void aGroupMessageIsTitledWithTheGroupAndNamesTheSender() {
        PushMessage push = pushText.pushFor(UUID.randomUUID(), Conversation.group(ADA.id(), "Rustaceans"), message("lunch?", null), 1);

        assertThat(push.title()).isEqualTo("Rustaceans");
        assertThat(push.body()).isEqualTo("Ada: lunch?");
    }

    @Test
    void pushesForOneChatShareATagAndCountWhatIsUnread() {
        UUID conversationId = UUID.randomUUID();
        Conversation direct = Conversation.direct(ADA.id(), UUID.randomUUID());

        PushMessage first = pushText.pushFor(conversationId, direct, message("one", null), 1);
        PushMessage third = pushText.pushFor(conversationId, direct, message("three", null), 3);

        assertThat(third.group()).isEqualTo(first.group()).isEqualTo("chat:" + conversationId);
        assertThat(third.title()).isEqualTo("Ada (3 new)");
        assertThat(third.body()).isEqualTo("three");
    }

    @Test
    void codeOnlyMessagesAndLongTextGetAShortPreview() {
        Conversation direct = Conversation.direct(ADA.id(), UUID.randomUUID());

        assertThat(pushText.pushFor(UUID.randomUUID(), direct, message(null, "fn main() {}"), 1).body()).isEqualTo("Sent a code snippet");
        assertThat(pushText.pushFor(UUID.randomUUID(), direct, message("x".repeat(150), null), 1).body()).hasSize(101).endsWith("…");
    }

    private static ResourceBundleMessageSource messages() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }

    private static MessageView message(String body, String code) {
        return new MessageView(UUID.randomUUID(), 1, ADA, MessageKind.TEXT, body, code, null, Instant.now(), false,
                UUID.randomUUID(), null, null);
    }
}
