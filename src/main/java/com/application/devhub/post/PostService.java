package com.application.devhub.post;

import com.application.devhub.block.Reachability;
import com.application.devhub.common.api.ApiException;
import com.application.devhub.notification.ActivityPublisher;
import com.application.devhub.notification.NotificationWriter;
import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.community.Community;
import com.application.devhub.community.CommunityAccess;
import com.application.devhub.community.CommunityMember;
import com.application.devhub.ratelimit.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository likeRepository;
    private final ActivityPublisher activityPublisher;
    private final NotificationWriter notificationWriter;
    private final Reachability reachability;
    private final CommunityAccess communityAccess;

    @Transactional
    public UUID create(UUID authorId, CreatePostRequest request) {
        Post post = request.replyToId() == null
                ? Post.original(authorId, request.content(), request.communityId())
                : Post.replyTo(reachablePost(authorId, request.replyToId()), authorId, request.content());
        if (post.getCommunityId() != null) {
            enterCommunity(post, authorId);
        }
        UUID id = postRepository.saveAndFlush(post).getId();
        if (post.isReply()) {
            activityPublisher.replied(authorId, id);
        } else {
            activityPublisher.posted(authorId, id);
        }
        return id;
    }

    @Transactional
    public void delete(UUID userId, UUID postId) {
        Post post = postRepository.findById(postId).orElseThrow(ApiException::notFound);
        if (!post.isAuthoredBy(userId)) {
            throw ApiException.forbidden();
        }
        post.delete();
        notificationWriter.withdrawSubject(postId);
    }

    @Transactional
    public void like(UUID userId, UUID postId) {
        reachablePost(userId, postId);
        if (likeRepository.like(postId, userId) > 0) {
            activityPublisher.likeChanged(userId, postId);
        }
    }

    @Transactional
    public void unlike(UUID userId, UUID postId) {
        if (likeRepository.unlike(postId, userId) > 0) {
            activityPublisher.likeChanged(userId, postId);
        }
    }

    private void enterCommunity(Post post, UUID authorId) {
        Community community = communityAccess.live(post.getCommunityId());
        communityAccess.ensureNotBanned(community.getId(), authorId);
        CommunityMember member = communityAccess.member(community.getId(), authorId);
        if (!post.isReply() && !member.canModerate() && community.getSlowModeSeconds() > 0
                && member.getLastPostedAt() != null) {
            Instant allowedFrom = member.getLastPostedAt().plusSeconds(community.getSlowModeSeconds());
            if (allowedFrom.isAfter(Instant.now())) {
                throw new RateLimitExceededException(ErrorCode.SLOW_MODE_ACTIVE, Duration.between(Instant.now(), allowedFrom));
            }
        }
        if (!post.isReply()) {
            member.posted();
        }
    }

    private Post reachablePost(UUID userId, UUID postId) {
        Post post = livePost(postId);
        if (!reachability.canReach(userId, post.getAuthorId())) {
            throw ApiException.notFound();
        }
        return post;
    }

    private Post livePost(UUID postId) {
        return postRepository.findById(postId).filter(post -> !post.isDeleted()).orElseThrow(ApiException::notFound);
    }
}
