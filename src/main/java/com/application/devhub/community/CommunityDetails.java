package com.application.devhub.community;

import java.util.Set;

public record CommunityDetails(String name, String description, JoinPolicy joinPolicy, Set<String> topics) {

    public CommunityDetails {
        topics = Set.copyOf(topics);
    }
}
