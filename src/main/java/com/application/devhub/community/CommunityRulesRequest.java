package com.application.devhub.community;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CommunityRulesRequest(
        @Schema(description = "The full list of rules in display order, at most 10; an empty list removes them all")
        @NotNull
        @Size(max = 10)
        List<@Valid @NotNull Rule> rules) {

    public record Rule(
            @Schema(description = "Short rule, up to 80 characters", example = "Be kind")
            @NotBlank
            @Size(max = 80)
            String title,
            @Schema(description = "What the rule means, up to 300 characters",
                    example = "Critique code, not people. No harassment.")
            @Size(max = 300)
            String body) {

        public Rule {
            title = title == null ? null : title.strip();
            body = body == null || body.isBlank() ? null : body.strip();
        }
    }
}
