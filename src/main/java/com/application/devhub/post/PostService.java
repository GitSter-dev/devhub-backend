package com.application.devhub.post;

import com.application.devhub.common.api.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository likeRepository;

    @Transactional
    public UUID create(UUID authorId, CreatePostRequest request) {
        Post post = request.replyToId() == null
                ? Post.original(authorId, request.content())
                : Post.replyTo(livePost(request.replyToId()), authorId, request.content());
        return postRepository.saveAndFlush(post).getId();
    }

    @Transactional
    public void delete(UUID userId, UUID postId) {
        Post post = postRepository.findById(postId).orElseThrow(ApiException::notFound);
        if (!post.isAuthoredBy(userId)) {
            throw ApiException.forbidden();
        }
        post.delete();
    }

    @Transactional
    public void like(UUID userId, UUID postId) {
        livePost(postId);
        likeRepository.like(postId, userId);
    }

    @Transactional
    public void unlike(UUID userId, UUID postId) {
        likeRepository.unlike(postId, userId);
    }

    private Post livePost(UUID postId) {
        return postRepository.findById(postId).filter(post -> !post.isDeleted()).orElseThrow(ApiException::notFound);
    }
}
