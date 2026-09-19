package com.application.devhub.block;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.follow.FollowRepository;
import com.application.devhub.notification.NotificationWriter;
import com.application.devhub.user.User;
import com.application.devhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockRepository blockRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final NotificationWriter notificationWriter;
    private final ApplicationEventPublisher events;

    @Transactional
    public void block(UUID blockerId, UUID blockedId) {
        if (blockerId.equals(blockedId)) {
            throw ApiException.of(ErrorCode.CANNOT_BLOCK_SELF);
        }
        userRepository.findById(blockedId).filter(User::isEmailVerified).orElseThrow(ApiException::notFound);
        if (blockRepository.block(blockerId, blockedId) == 0) {
            return;
        }
        followRepository.unfollow(blockerId, blockedId);
        followRepository.unfollow(blockedId, blockerId);
        notificationWriter.withdrawBetween(blockerId, blockedId);
        events.publishEvent(new BlockChanged(blockerId, blockedId, true));
    }

    @Transactional
    public void unblock(UUID blockerId, UUID blockedId) {
        if (blockRepository.unblock(blockerId, blockedId) > 0) {
            events.publishEvent(new BlockChanged(blockerId, blockedId, false));
        }
    }

    @Transactional(readOnly = true)
    public boolean blockedBetween(UUID first, UUID second) {
        return blockRepository.existsBetween(first, second);
    }
}
