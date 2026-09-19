package com.application.devhub.account;

import com.application.devhub.chat.ConversationMemberRepository;
import com.application.devhub.chat.ConversationService;
import com.application.devhub.user.HeldUsernameRepository;
import com.application.devhub.user.User;
import com.application.devhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountPurger {

    private static final String DELETED_DISPLAY_NAME = "Deleted user";
    private static final String DELETED_EMAIL_DOMAIN = "@deleted.devhub.invalid";
    private static final int HANDLE_LENGTH = 8;

    private final UserRepository userRepository;
    private final AccountPurgeRepository purgeRepository;
    private final HeldUsernameRepository heldUsernames;
    private final ConversationMemberRepository memberRepository;
    private final ConversationService conversationService;
    private final AccountProperties properties;

    @Transactional
    public int purgeDue() {
        List<User> due = userRepository.findDueForPurge(Instant.now().minus(properties.gracePeriod()),
                PageRequest.of(0, properties.purgeBatchSize()));
        due.forEach(this::purge);
        if (!due.isEmpty()) {
            log.info("Anonymized {} deleted accounts", due.size());
        }
        return due.size();
    }

    private void purge(User user) {
        UUID userId = user.getId();
        leaveGroups(userId);
        purgeRepository.erasePosts(userId);
        purgeRepository.eraseMessages(userId);
        purgeRepository.deleteLikes(userId);
        purgeRepository.deleteFollows(userId);
        purgeRepository.deleteBlocks(userId);
        purgeRepository.deleteTopics(userId);
        purgeRepository.deleteDevices(userId);
        purgeRepository.deleteRefreshTokens(userId);
        purgeRepository.deleteNotificationActors(userId);
        purgeRepository.deleteNotifications(userId);
        heldUsernames.hold(user.getUsername(), userId, Instant.now().plus(properties.usernameHold()));
        user.anonymize(anonymousHandle(userId), userId + DELETED_EMAIL_DOMAIN, DELETED_DISPLAY_NAME);
    }

    private void leaveGroups(UUID userId) {
        memberRepository.activeGroupsOf(userId)
                .forEach(conversationId -> conversationService.removeMember(userId, conversationId, userId));
    }

    private static String anonymousHandle(UUID userId) {
        return "deleted_" + userId.toString().replace("-", "").substring(0, HANDLE_LENGTH);
    }
}
