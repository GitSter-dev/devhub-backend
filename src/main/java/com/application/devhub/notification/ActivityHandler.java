package com.application.devhub.notification;

import com.application.devhub.chat.ConversationMember;
import com.application.devhub.chat.ConversationMemberRepository;
import com.application.devhub.chat.MemberStatus;
import com.application.devhub.follow.FollowRepository;
import com.application.devhub.outbox.OutboxEvent;
import com.application.devhub.outbox.OutboxEventHandler;
import com.application.devhub.outbox.OutboxEventType;
import com.application.devhub.post.Post;
import com.application.devhub.post.PostLike;
import com.application.devhub.post.PostLikeRepository;
import com.application.devhub.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ActivityHandler implements OutboxEventHandler {

    private static final UUID FIRST_FOLLOWER = new UUID(0L, 0L);

    private final JsonMapper jsonMapper;
    private final NotificationWriter writer;
    private final ActivityPublisher publisher;
    private final NotificationProperties properties;
    private final PostRepository postRepository;
    private final PostLikeRepository likeRepository;
    private final FollowRepository followRepository;
    private final ConversationMemberRepository memberRepository;

    @Override
    public OutboxEventType type() {
        return OutboxEventType.NOTIFICATION;
    }

    @Override
    public void handle(OutboxEvent event) {
        Activity activity = jsonMapper.readValue(event.getPayload(), Activity.class);
        switch (activity.kind()) {
            case LIKE -> like(activity);
            case REPLY -> reply(activity);
            case FOLLOW -> follow(activity);
            case POST -> post(activity);
            case MESSAGE_REQUEST -> messageRequest(activity);
        }
    }

    private void like(Activity activity) {
        Optional<Post> found = postRepository.findById(activity.targetId());
        if (found.isEmpty()) {
            return;
        }
        Post post = found.get();
        boolean liked = !post.isDeleted() && !post.isAuthoredBy(activity.actorId())
                && likeRepository.existsById(new PostLike.Key(post.getId(), activity.actorId()));
        if (liked) {
            writer.record(List.of(post.getAuthorId()), NotificationType.POST_LIKED, post.getId(), activity.actorId(),
                    post.getId());
        } else {
            writer.withdraw(post.getAuthorId(), NotificationType.POST_LIKED, post.getId(), activity.actorId());
        }
    }

    private void reply(Activity activity) {
        Post reply = livePost(activity.targetId()).filter(Post::isReply).orElse(null);
        if (reply == null) {
            return;
        }
        livePost(reply.getParentId())
                .filter(parent -> !parent.isAuthoredBy(activity.actorId()))
                .ifPresent(parent -> writer.record(List.of(parent.getAuthorId()), NotificationType.POST_REPLIED,
                        parent.getId(), activity.actorId(), reply.getId()));
    }

    private void follow(Activity activity) {
        if (activity.actorId().equals(activity.targetId())) {
            return;
        }
        if (followRepository.isFollowing(activity.actorId(), activity.targetId())) {
            writer.record(List.of(activity.targetId()), NotificationType.NEW_FOLLOWER, null, activity.actorId(),
                    activity.actorId());
        } else {
            writer.withdraw(activity.targetId(), NotificationType.NEW_FOLLOWER, null, activity.actorId());
        }
    }

    private void post(Activity activity) {
        if (livePost(activity.targetId()).filter(post -> !post.isReply()).isEmpty()) {
            return;
        }
        UUID after = activity.cursor() == null ? FIRST_FOLLOWER : activity.cursor();
        List<UUID> followers = followRepository.followerIdsAfter(activity.actorId(), after, properties.fanOutChunk());
        writer.record(followers, NotificationType.FOLLOWED_POSTED, null, activity.actorId(), activity.targetId());
        if (followers.size() == properties.fanOutChunk()) {
            publisher.continuePost(activity, followers.getLast());
        }
    }

    private void messageRequest(Activity activity) {
        List<ConversationMember> members = memberRepository.findAllMembers(activity.targetId());
        boolean requesterActive = members.stream()
                .anyMatch(member -> member.userId().equals(activity.actorId()) && member.isActive());
        members.stream()
                .filter(member -> !member.userId().equals(activity.actorId()))
                .forEach(member -> {
                    if (requesterActive && member.getStatus() == MemberStatus.REQUEST) {
                        writer.record(List.of(member.userId()), NotificationType.MESSAGE_REQUEST, null,
                                activity.actorId(), activity.targetId());
                    } else {
                        writer.withdraw(member.userId(), NotificationType.MESSAGE_REQUEST, null, activity.actorId());
                    }
                });
    }

    private Optional<Post> livePost(UUID postId) {
        return postRepository.findById(postId).filter(post -> !post.isDeleted());
    }
}
