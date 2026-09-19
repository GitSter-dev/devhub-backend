package com.application.devhub.chat;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.follow.FollowRepository;
import com.application.devhub.notification.ActivityPublisher;
import com.application.devhub.user.User;
import com.application.devhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    public static final int MAX_GROUP_MEMBERS = 50;

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final MessageLog messageLog;
    private final ReceiptService receiptService;
    private final ApplicationEventPublisher events;
    private final ActivityPublisher activityPublisher;

    @Transactional
    public Opened openDirect(UUID me, UUID otherId) {
        if (me.equals(otherId)) {
            throw ApiException.of(ErrorCode.CANNOT_MESSAGE_YOURSELF);
        }
        requireVerified(List.of(otherId));
        return conversationRepository.findByDirectKey(Conversation.directKey(me, otherId))
                .map(existing -> reopen(existing, me))
                .orElseGet(() -> startDirect(me, otherId));
    }

    @Transactional
    public UUID createGroup(UUID owner, String title, List<UUID> memberIds) {
        Set<UUID> others = distinctWithout(memberIds, owner);
        if (others.size() + 1 > MAX_GROUP_MEMBERS) {
            throw ApiException.of(ErrorCode.GROUP_TOO_LARGE);
        }
        requireVerified(others);
        Conversation group = conversationRepository.save(Conversation.group(owner, title));
        memberRepository.save(ConversationMember.of(group.getId(), owner, MemberRole.OWNER, MemberStatus.ACTIVE));
        others.forEach(userId -> memberRepository.save(
                ConversationMember.of(group.getId(), userId, MemberRole.MEMBER, MemberStatus.ACTIVE)));
        memberRepository.flush();
        Message created = messageLog.appendSystem(group.getId(), SystemEventType.GROUP_CREATED, owner, null, title);
        List<UUID> everyone = new ArrayList<>(others);
        everyone.add(owner);
        receiptService.catchUp(group.getId(), everyone, created.getSeq());
        announce(group.getId());
        return group.getId();
    }

    @Transactional
    public void rename(UUID me, UUID conversationId, String title) {
        Conversation group = ownedGroup(me, conversationId);
        group.rename(title);
        Message renamed = messageLog.appendSystem(conversationId, SystemEventType.GROUP_RENAMED, me, null, title);
        receiptService.catchUp(conversationId, List.of(me), renamed.getSeq());
        announce(conversationId);
    }

    @Transactional
    public void addMembers(UUID me, UUID conversationId, List<UUID> userIds) {
        ownedGroup(me, conversationId);
        Set<UUID> candidates = distinctWithout(userIds, me);
        requireVerified(candidates);
        List<UUID> joining = candidates.stream()
                .filter(userId -> memberRepository.find(conversationId, userId).map(member -> !member.isActive()).orElse(true))
                .toList();
        if (memberRepository.countActive(conversationId) + joining.size() > MAX_GROUP_MEMBERS) {
            throw ApiException.of(ErrorCode.GROUP_TOO_LARGE);
        }
        long lastSeq = 0;
        for (UUID userId : joining) {
            memberRepository.find(conversationId, userId).ifPresentOrElse(ConversationMember::activate,
                    () -> memberRepository.save(ConversationMember.of(conversationId, userId, MemberRole.MEMBER, MemberStatus.ACTIVE)));
            memberRepository.flush();
            lastSeq = messageLog.appendSystem(conversationId, SystemEventType.MEMBER_ADDED, me, userId, null).getSeq();
        }
        List<UUID> caughtUp = new ArrayList<>(joining);
        caughtUp.add(me);
        receiptService.catchUp(conversationId, caughtUp, lastSeq);
        announce(conversationId);
    }

    @Transactional
    public void removeMember(UUID me, UUID conversationId, UUID target) {
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow(ApiException::notFound);
        ConversationMember actor = activeMember(conversationId, me);
        if (!conversation.isGroup()) {
            throw ApiException.badRequest();
        }
        if (me.equals(target)) {
            leave(conversationId, actor);
        } else {
            if (!actor.isOwner()) {
                throw ApiException.of(ErrorCode.NOT_GROUP_OWNER);
            }
            activeMember(conversationId, target).leave();
            messageLog.appendSystem(conversationId, SystemEventType.MEMBER_REMOVED, me, target, null);
        }
        announce(conversationId);
    }

    @Transactional
    public void accept(UUID me, UUID conversationId) {
        ConversationMember member = memberRepository.find(conversationId, me)
                .filter(candidate -> candidate.getStatus() == MemberStatus.REQUEST || candidate.isActive())
                .orElseThrow(ApiException::notFound);
        member.activate();
        reconcileRequests(conversationId, me);
        announce(conversationId);
    }

    @Transactional
    public void decline(UUID me, UUID conversationId) {
        memberRepository.find(conversationId, me)
                .filter(candidate -> candidate.getStatus() == MemberStatus.REQUEST)
                .orElseThrow(ApiException::notFound)
                .decline();
        reconcileRequests(conversationId, me);
        announce(conversationId);
    }

    private Opened reopen(Conversation existing, UUID me) {
        ConversationMember mine = memberRepository.find(existing.getId(), me).orElseThrow(ApiException::notFound);
        if (mine.getStatus() == MemberStatus.DECLINED) {
            mine.activate();
            announce(existing.getId());
        }
        return new Opened(existing.getId(), false);
    }

    private Opened startDirect(UUID me, UUID otherId) {
        Conversation direct = conversationRepository.save(Conversation.direct(me, otherId));
        MemberStatus theirs = followRepository.isFollowing(otherId, me) ? MemberStatus.ACTIVE : MemberStatus.REQUEST;
        memberRepository.save(ConversationMember.of(direct.getId(), me, MemberRole.MEMBER, MemberStatus.ACTIVE));
        memberRepository.save(ConversationMember.of(direct.getId(), otherId, MemberRole.MEMBER, theirs));
        announce(direct.getId());
        return new Opened(direct.getId(), true);
    }

    private void leave(UUID conversationId, ConversationMember member) {
        boolean wasOwner = member.isOwner();
        member.leave();
        memberRepository.flush();
        messageLog.appendSystem(conversationId, SystemEventType.MEMBER_LEFT, member.userId(), null, null);
        if (wasOwner) {
            memberRepository.findActive(conversationId).stream().findFirst().ifPresent(successor -> {
                successor.makeOwner();
                messageLog.appendSystem(conversationId, SystemEventType.OWNER_CHANGED, null, successor.userId(), null);
            });
        }
    }

    private Conversation ownedGroup(UUID me, UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow(ApiException::notFound);
        ConversationMember member = activeMember(conversationId, me);
        if (!conversation.isGroup()) {
            throw ApiException.badRequest();
        }
        if (!member.isOwner()) {
            throw ApiException.of(ErrorCode.NOT_GROUP_OWNER);
        }
        return conversation;
    }

    private ConversationMember activeMember(UUID conversationId, UUID userId) {
        return memberRepository.find(conversationId, userId)
                .filter(ConversationMember::isActive)
                .orElseThrow(ApiException::notFound);
    }

    private void requireVerified(Iterable<UUID> userIds) {
        for (UUID userId : userIds) {
            userRepository.findById(userId).filter(User::isEmailVerified).orElseThrow(ApiException::notFound);
        }
    }

    private static Set<UUID> distinctWithout(List<UUID> userIds, UUID excluded) {
        Set<UUID> distinct = new LinkedHashSet<>(userIds);
        distinct.remove(excluded);
        return distinct;
    }

    private void reconcileRequests(UUID conversationId, UUID recipient) {
        memberRepository.findAllMembers(conversationId).stream()
                .map(ConversationMember::userId)
                .filter(userId -> !userId.equals(recipient))
                .forEach(requester -> activityPublisher.requestChanged(requester, conversationId));
    }

    private void announce(UUID conversationId) {
        events.publishEvent(new ChatEvents.ConversationChanged(conversationId));
    }

    public record Opened(UUID conversationId, boolean created) {
    }
}
