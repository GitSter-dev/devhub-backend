package com.application.devhub.community;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CommunityDeparture {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;
    private final CommunityJoinRequestRepository requestRepository;

    @Transactional
    public void leaveEverything(UUID userId) {
        for (CommunityMember membership : memberRepository.membershipsOf(userId)) {
            UUID communityId = membership.getKey().communityId();
            if (membership.isOwner()) {
                handOverOrClose(communityId, userId);
            }
            memberRepository.delete(membership);
            memberRepository.flush();
            communityRepository.adjustMemberCount(communityId, -1);
        }
        requestRepository.deletePendingOf(userId);
    }

    private void handOverOrClose(UUID communityId, UUID leavingOwnerId) {
        Community community = communityRepository.findById(communityId).orElseThrow();
        memberRepository.successorsOf(communityId, leavingOwnerId, Limit.of(1)).stream().findFirst().ifPresentOrElse(
                successor -> {
                    CommunityMember leaving = memberRepository.findById(new CommunityMember.Key(communityId, leavingOwnerId))
                            .orElseThrow();
                    leaving.stepDown();
                    memberRepository.saveAndFlush(leaving);
                    successor.becomeOwner();
                    community.handOverTo(successor.userId());
                },
                community::takeDown);
    }
}
