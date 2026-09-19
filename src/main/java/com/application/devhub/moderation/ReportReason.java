package com.application.devhub.moderation;

public enum ReportReason {
    SPAM(1),
    IMPERSONATION(2),
    HARASSMENT(3),
    HATE(3),
    SEXUAL(4),
    VIOLENCE(4),
    SELF_HARM(5),
    OTHER(1);

    private final int weight;

    ReportReason(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
