package com.application.devhub.block;

import com.application.devhub.user.User;
import com.application.devhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class Reachability {

    private final UserRepository userRepository;
    private final BlockRepository blockRepository;

    @Transactional(readOnly = true)
    public boolean canReach(UUID viewerId, User other) {
        return other.getId().equals(viewerId)
                || (other.isVisible() && !blockRepository.existsBetween(viewerId, other.getId()));
    }

    @Transactional(readOnly = true)
    public boolean canReach(UUID viewerId, UUID otherId) {
        return otherId.equals(viewerId) || userRepository.findById(otherId).map(other -> canReach(viewerId, other))
                .orElse(false);
    }
}
