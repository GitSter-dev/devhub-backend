package com.application.devhub.user;

import com.application.devhub.common.api.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;
    private final UsernameProperties usernameProperties;

    @Transactional(readOnly = true)
    public CurrentUserResponse get(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> CurrentUserResponse.from(user, usernameProperties.changeCooldown()))
                .orElseThrow(ApiException::notFound);
    }
}
