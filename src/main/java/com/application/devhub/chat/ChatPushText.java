package com.application.devhub.chat;

import com.application.devhub.chat.ChatViews.MessageView;
import com.application.devhub.push.PushMessage;
import com.application.devhub.push.PushProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatPushText {

    private static final String KIND = "chat";
    private static final int PREVIEW_LENGTH = 100;

    private final MessageSource messageSource;
    private final PushProperties properties;

    public PushMessage pushFor(UUID conversationId, Conversation conversation, MessageView message, long unread) {
        String preview = previewOf(message);
        String sender = message.sender().displayName();
        String name = conversation.isGroup() ? conversation.getTitle() : sender;
        String title = unread > 1 ? text("push.chat.title-unread", name, unread) : name;
        String body = conversation.isGroup() ? sender + ": " + preview : preview;
        return PushMessage.grouped(title, body, Map.of("kind", KIND, "conversationId", conversationId.toString(),
                "messageId", message.id().toString()), KIND + ":" + conversationId);
    }

    private String previewOf(MessageView message) {
        if (message.body() == null) {
            return text("push.chat.code");
        }
        return message.body().length() <= PREVIEW_LENGTH ? message.body() : message.body().substring(0, PREVIEW_LENGTH) + "…";
    }

    private String text(String key, Object... arguments) {
        return messageSource.getMessage(key, arguments, properties.locale());
    }
}
