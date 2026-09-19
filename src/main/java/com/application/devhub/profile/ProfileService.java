package com.application.devhub.profile;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.follow.FollowRepository;
import com.application.devhub.topic.UserTopics;
import com.application.devhub.user.HeldUsername;
import com.application.devhub.user.HeldUsernameRepository;
import com.application.devhub.user.User;
import com.application.devhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final HeldUsernameRepository heldUsernames;
    private final FollowRepository followRepository;
    private final UserTopics userTopics;

    @Transactional(readOnly = true)
    public ProfileResponse view(UUID viewerId, String username) {
        return toResponse(viewerId, visibleUser(username));
    }

    @Transactional
    public ProfileResponse update(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId).orElseThrow(ApiException::notFound);
        user.updateProfile(request.toDetails());
        return toResponse(userId, userRepository.saveAndFlush(user));
    }

    @Transactional(readOnly = true)
    public User visibleUser(String username) {
        return userRepository.findByUsername(username)
                .or(() -> heldUsernames.findActive(username, Instant.now())
                        .map(HeldUsername::getUserId)
                        .flatMap(userRepository::findById))
                .filter(User::isEmailVerified)
                .orElseThrow(ApiException::notFound);
    }

    private ProfileResponse toResponse(UUID viewerId, User user) {
        UUID id = user.getId();
        boolean me = id.equals(viewerId);
        return new ProfileResponse(id, user.getUsername(), user.getDisplayName(), user.getBio(),
                user.getGithubUsername(), user.getWebsiteUrl(), userTopics.of(id),
                followRepository.countFollowers(id), followRepository.countFollowing(id), user.getCreatedAt(), me,
                !me && followRepository.isFollowing(viewerId, id), !me && followRepository.isFollowing(id, viewerId));
    }
}
