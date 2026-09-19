package com.application.devhub.topic;

public record TopicResponse(String slug, String name) {

    public static TopicResponse from(Topic topic) {
        return new TopicResponse(topic.getSlug(), topic.getName());
    }
}
