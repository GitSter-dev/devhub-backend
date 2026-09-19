package com.application.devhub.topic;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Objects;

public record MyTopicsRequest(
        @Schema(description = "Topic slugs from GET /topics; duplicates are ignored", example = "[\"rust\", \"go\"]")
        @NotNull
        @Size(min = MyTopicsRequest.MIN_TOPICS, max = MyTopicsRequest.MAX_TOPICS)
        @KnownTopics
        List<String> slugs) {

    static final int MIN_TOPICS = 1;
    static final int MAX_TOPICS = 10;

    public MyTopicsRequest {
        slugs = slugs == null ? null : slugs.stream().filter(Objects::nonNull).map(String::strip).distinct().toList();
    }
}
