package com.application.devhub.user;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.topic.UserTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSetupService {

    private final UserRepository userRepository;
    private final UsernameProperties usernameProperties;
    private final UserTopics userTopics;

    @Transactional
    public CurrentUserResponse complete(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(ApiException::notFound);
        if (!userTopics.hasAny(userId)) {
            throw ApiException.of(ErrorCode.TOPICS_REQUIRED);
        }
        user.completeSetup();
        return CurrentUserResponse.from(user, usernameProperties.changeCooldown());
    }
}
