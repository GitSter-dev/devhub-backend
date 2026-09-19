package com.application.devhub.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;
import java.util.UUID;

public record SendMessageRequest(
        @Schema(description = "Random UUID the app generates per message; resending it never duplicates the message")
        @NotNull
        UUID clientMessageId,

        @Schema(description = "The text, up to 4000 characters", example = "Pushed the fix, can you review?")
        @Size(max = 4000)
        String body,

        @Schema(description = "An optional code block, up to 4000 characters")
        @Size(max = 4000)
        String code,

        @Schema(description = "Language label for the code block", example = "rust")
        @Size(max = 20)
        @Pattern(regexp = "^[a-z0-9+#.-]+$", message = "may only contain lowercase letters, digits and + # . -")
        String codeLanguage,

        @Schema(description = "A message in this conversation to reply to")
        UUID replyToId) {

    public SendMessageRequest {
        body = body == null || body.isBlank() ? null : body.strip();
        code = code == null || code.isBlank() ? null : code.stripTrailing();
        codeLanguage = code == null || codeLanguage == null || codeLanguage.isBlank()
                ? null
                : codeLanguage.strip().toLowerCase(Locale.ROOT);
    }

    @Schema(hidden = true)
    @AssertTrue(message = "write something or add a code block")
    public boolean isBodyOrCodePresent() {
        return body != null || code != null;
    }

    MessageContent content() {
        return new MessageContent(body, code, codeLanguage);
    }
}
