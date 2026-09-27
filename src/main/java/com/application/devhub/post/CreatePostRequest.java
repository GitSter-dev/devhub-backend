package com.application.devhub.post;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;
import java.util.UUID;

public record CreatePostRequest(
        @Schema(description = "The text, up to 500 characters", example = "Rewrote our sync engine around streams.")
        @Size(max = 500)
        String body,

        @Schema(description = "An optional code block, up to 4000 characters",
                example = "const feed = await devhub.follow([\"rust\"]).stream();")
        @Size(max = 4000)
        String code,

        @Schema(description = "Language label for the code block", example = "typescript")
        @Size(max = 20)
        @Pattern(regexp = "^[a-z0-9+#.-]+$", message = "may only contain lowercase letters, digits and + # . -")
        String codeLanguage,

        @Schema(description = "The post this replies to; omit for an original post")
        UUID replyToId,
        @Schema(description = "Community to post into; you must be a member. Ignored for replies, which stay in "
                + "their thread's community")
        UUID communityId) {

    public CreatePostRequest {
        body = blankToNull(body, true);
        code = blankToNull(code, false);
        codeLanguage = code == null ? null : blankToNull(codeLanguage, true);
        codeLanguage = codeLanguage == null ? null : codeLanguage.toLowerCase(Locale.ROOT);
    }

    @Schema(hidden = true)
    @AssertTrue(message = "write something or add a code block")
    public boolean isBodyOrCodePresent() {
        return body != null || code != null;
    }

    PostContent content() {
        return new PostContent(body, code, codeLanguage);
    }

    private static String blankToNull(String value, boolean strip) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return strip ? value.strip() : value.stripTrailing();
    }
}
