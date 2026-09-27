package com.application.devhub.community;

import com.application.devhub.common.api.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityStaffService {

    private final CommunityAccess access;
    private final CommunityMemberRepository memberRepository;

    @Transactional
    public void appoint(UUID ownerId, String slug, UUID userId) {
        Community community = access.live(slug);
        access.owner(community.getId(), ownerId);
        access.member(community.getId(), userId).promote();
    }

    @Transactional
    public void dismiss(UUID ownerId, String slug, UUID userId) {
        Community community = access.live(slug);
        access.owner(community.getId(), ownerId);
        access.member(community.getId(), userId).demote();
    }

    @Transactional
    public void handOver(UUID ownerId, String slug, UUID userId) {
        Community community = access.live(slug);
        CommunityMember owner = access.owner(community.getId(), ownerId);
        CommunityMember successor = access.moderator(community.getId(), userId);
        if (successor.isOwner()) {
            return;
        }
        if (!successor.canModerate()) {
            throw ApiException.badRequest();
        }
        owner.stepDown();
        memberRepository.saveAndFlush(owner);
        successor.becomeOwner();
        community.handOverTo(userId);
    }
}
