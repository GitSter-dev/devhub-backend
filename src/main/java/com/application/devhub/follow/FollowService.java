package com.application.devhub.follow;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.notification.ActivityPublisher;
import com.application.devhub.user.User;
import com.application.devhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final ActivityPublisher activityPublisher;

    @Transactional
    public void follow(UUID followerId, UUID followeeId) {
        if (followerId.equals(followeeId)) {
            throw ApiException.of(ErrorCode.CANNOT_FOLLOW_SELF);
        }
        userRepository.findById(followeeId)
                .filter(User::isEmailVerified)
                .orElseThrow(ApiException::notFound);
        if (followRepository.follow(followerId, followeeId) > 0) {
            activityPublisher.followChanged(followerId, followeeId);
        }
    }

    @Transactional
    public void unfollow(UUID followerId, UUID followeeId) {
        if (followRepository.unfollow(followerId, followeeId) > 0) {
            activityPublisher.followChanged(followerId, followeeId);
        }
    }
}
