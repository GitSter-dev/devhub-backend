package com.application.devhub.suggestion;

import java.util.List;
import java.util.UUID;

public record SuggestionResponse(SuggestedUser user, Reason reason) {

    static SuggestionResponse of(UUID id, String username, String displayName, List<String> sharedTopics) {
        Reason reason = sharedTopics.isEmpty()
                ? new Reason(SuggestionReasonType.POPULAR, List.of())
                : new Reason(SuggestionReasonType.SHARED_TOPICS, sharedTopics);
        return new SuggestionResponse(new SuggestedUser(id, username, displayName), reason);
    }

    public record SuggestedUser(UUID id, String username, String displayName) {
    }

    public record Reason(SuggestionReasonType type, List<String> topics) {
    }
}
