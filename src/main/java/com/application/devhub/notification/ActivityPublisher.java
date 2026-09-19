package com.application.devhub.notification;

import com.application.devhub.outbox.OutboxEventType;
import com.application.devhub.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ActivityPublisher {

    private final OutboxPublisher outboxPublisher;

    public void likeChanged(UUID userId, UUID postId) {
        publish(new Activity(Activity.Kind.LIKE, userId, postId, null));
    }

    public void replied(UUID authorId, UUID replyId) {
        publish(new Activity(Activity.Kind.REPLY, authorId, replyId, null));
    }

    public void followChanged(UUID followerId, UUID followeeId) {
        publish(new Activity(Activity.Kind.FOLLOW, followerId, followeeId, null));
    }

    public void posted(UUID authorId, UUID postId) {
        publish(new Activity(Activity.Kind.POST, authorId, postId, null));
    }

    public void requestChanged(UUID requesterId, UUID conversationId) {
        publish(new Activity(Activity.Kind.MESSAGE_REQUEST, requesterId, conversationId, null));
    }

    void continuePost(Activity activity, UUID afterFollowerId) {
        publish(new Activity(Activity.Kind.POST, activity.actorId(), activity.targetId(), afterFollowerId));
    }

    private void publish(Activity activity) {
        outboxPublisher.publish(OutboxEventType.NOTIFICATION, activity);
    }
}
