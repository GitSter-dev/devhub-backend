package com.application.devhub.chat;

import com.application.devhub.chat.ChatViews.MessageView;
import com.application.devhub.chat.ChatViews.PersonRef;
import com.application.devhub.push.PushMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ChatPushHandlerTest {

    private static final PersonRef ADA = new PersonRef(UUID.randomUUID(), "ada", "Ada");

    @Test
    void aDirectMessageIsTitledWithTheSender() {
        PushMessage push = ChatPushHandler.pushFor(UUID.randomUUID(), Conversation.direct(ADA.id(), UUID.randomUUID()), message("hey there", null));

        assertThat(push.title()).isEqualTo("Ada");
        assertThat(push.body()).isEqualTo("hey there");
        assertThat(push.data()).containsEntry("kind", "chat");
    }

    @Test
    void aGroupMessageIsTitledWithTheGroupAndNamesTheSender() {
        PushMessage push = ChatPushHandler.pushFor(UUID.randomUUID(), Conversation.group(ADA.id(), "Rustaceans"), message("lunch?", null));

        assertThat(push.title()).isEqualTo("Rustaceans");
        assertThat(push.body()).isEqualTo("Ada: lunch?");
    }

    @Test
    void codeOnlyMessagesAndLongTextGetAShortPreview() {
        Conversation direct = Conversation.direct(ADA.id(), UUID.randomUUID());

        assertThat(ChatPushHandler.pushFor(UUID.randomUUID(), direct, message(null, "fn main() {}")).body()).isEqualTo("Sent a code snippet");
        assertThat(ChatPushHandler.pushFor(UUID.randomUUID(), direct, message("x".repeat(150), null)).body()).hasSize(101).endsWith("…");
    }

    private static MessageView message(String body, String code) {
        return new MessageView(UUID.randomUUID(), 1, ADA, MessageKind.TEXT, body, code, null, Instant.now(), false,
                UUID.randomUUID(), null, null);
    }
}
