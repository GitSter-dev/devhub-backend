package com.application.devhub.push;

import java.util.Map;

public record PushMessage(String title, String body, Map<String, String> data, String group) {

    public static PushMessage of(String title, String body, Map<String, String> data) {
        return new PushMessage(title, body, data, null);
    }

    public static PushMessage grouped(String title, String body, Map<String, String> data, String group) {
        return new PushMessage(title, body, data, group);
    }
}
