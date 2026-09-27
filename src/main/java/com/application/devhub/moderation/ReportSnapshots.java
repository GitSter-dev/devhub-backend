package com.application.devhub.moderation;

import com.application.devhub.block.Reachability;
import com.application.devhub.chat.ChatQueries;
import com.application.devhub.chat.ChatViews.MessageView;
import com.application.devhub.chat.MessageRepository;
import com.application.devhub.common.api.ApiException;
import com.application.devhub.post.PostView;
import com.application.devhub.post.PostViews;
import com.application.devhub.user.User;
import com.application.devhub.user.UserRepository;
import com.application.devhub.community.Community;
import com.application.devhub.community.CommunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class ReportSnapshots {

    private final PostViews postViews;
    private final ChatQueries chatQueries;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final Reachability reachability;
    private final ModerationProperties properties;
    private final JsonMapper jsonMapper;
    private final CommunityRepository communityRepository;

    public Captured capture(UUID reporterId, ReportTarget targetType, UUID targetId) {
        return switch (targetType) {
            case POST -> post(reporterId, targetId);
            case MESSAGE -> message(reporterId, targetId);
            case USER -> user(reporterId, targetId);
            case COMMUNITY -> community(reporterId, targetId);
        };
    }

    private Captured post(UUID reporterId, UUID postId) {
        PostView post = postViews.byId(reporterId, postId).orElseThrow(ApiException::notFound);
        if (post.mine()) {
            throw ApiException.badRequest();
        }
        return new Captured(post.author().id(), json(Map.of(
                "postId", post.id().toString(),
                "community", post.community() == null ? "" : post.community().slug(),
                "author", post.author().username(),
                "body", nullable(post.body()),
                "code", nullable(post.code()),
                "codeLanguage", nullable(post.codeLanguage()),
                "createdAt", post.createdAt().toString())), post.community() == null ? null : post.community().id());
    }

    private Captured community(UUID reporterId, UUID communityId) {
        Community community = communityRepository.findById(communityId)
                .filter(candidate -> !candidate.isRemoved())
                .orElseThrow(ApiException::notFound);
        if (community.getOwnerId().equals(reporterId)) {
            throw ApiException.badRequest();
        }
        return new Captured(community.getOwnerId(), json(Map.of(
                "communityId", community.getId().toString(),
                "slug", community.getSlug(),
                "name", community.getName(),
                "description", nullable(community.getDescription()))));
    }

    private Captured message(UUID reporterId, UUID messageId) {
        UUID conversationId = messageRepository.findById(messageId)
                .orElseThrow(ApiException::notFound)
                .getConversationId();
        chatQueries.conversation(reporterId, conversationId);
        MessageView message = chatQueries.message(messageId);
        if (message.sender() == null || message.sender().id().equals(reporterId)) {
            throw ApiException.badRequest();
        }
        List<MessageView> context = chatQueries
                .messages(reporterId, conversationId, message.seq(), null, properties.messageContextSize())
                .items();
        return new Captured(message.sender().id(), json(Map.of(
                "conversationId", conversationId.toString(),
                "messageId", message.id().toString(),
                "sender", message.sender().username(),
                "createdAt", message.createdAt().toString(),
                "messages", Stream.concat(context.stream(), Stream.of(message)).map(ReportSnapshots::messageLine).toList())));
    }

    private Captured user(UUID reporterId, UUID userId) {
        if (userId.equals(reporterId)) {
            throw ApiException.badRequest();
        }
        User user = userRepository.findById(userId)
                .filter(candidate -> reachability.canReach(reporterId, candidate))
                .orElseThrow(ApiException::notFound);
        return new Captured(user.getId(), json(Map.of(
                "userId", user.getId().toString(),
                "username", user.getUsername(),
                "displayName", user.getDisplayName(),
                "bio", nullable(user.getBio()),
                "githubUsername", nullable(user.getGithubUsername()),
                "websiteUrl", nullable(user.getWebsiteUrl()))));
    }

    private static Map<String, String> messageLine(MessageView message) {
        Map<String, String> line = new LinkedHashMap<>();
        line.put("seq", Long.toString(message.seq()));
        line.put("sender", message.sender() == null ? "" : message.sender().username());
        line.put("body", nullable(message.body()));
        line.put("code", nullable(message.code()));
        return line;
    }

    private static String nullable(String value) {
        return value == null ? "" : value;
    }

    private String json(Map<String, Object> fields) {
        return jsonMapper.writeValueAsString(fields);
    }

    public record Captured(UUID ownerId, String json, UUID communityId) {

        Captured(UUID ownerId, String json) {
            this(ownerId, json, null);
        }
    }
}
