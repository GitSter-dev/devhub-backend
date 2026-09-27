package com.application.devhub.community;

public enum CommunityRole {
    OWNER,
    MODERATOR,
    MEMBER;

    public boolean canModerate() {
        return this != MEMBER;
    }
}
