package com.application.devhub.community;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.common.metrics.DevHubMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final CommunityAccess access;
    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;
    private final CommunityJoinRequestRepository requestRepository;
    private final DevHubMetrics metrics;

    @Transactional
    public void join(UUID userId, String slug, JoinCommunityRequest request) {
        Community community = access.live(slug);
        if (access.membership(community.getId(), userId).isPresent()) {
            return;
        }
        if (community.getJoinPolicy() == JoinPolicy.OPEN) {
            admit(community.getId(), userId);
        } else if (pendingRequest(community.getId(), userId) == null) {
            requestRepository.save(CommunityJoinRequest.ask(community.getId(), userId, request.message()));
        }
        metrics.communityJoin(community.getJoinPolicy());
    }

    @Transactional
    public void leave(UUID userId, String slug) {
        Community community = access.live(slug);
        CommunityJoinRequest pending = pendingRequest(community.getId(), userId);
        if (pending != null) {
            requestRepository.delete(pending);
            return;
        }
        access.membership(community.getId(), userId).ifPresent(member -> {
            if (member.isOwner()) {
                throw ApiException.of(ErrorCode.OWNER_CANNOT_LEAVE);
            }
            memberRepository.delete(member);
            communityRepository.adjustMemberCount(community.getId(), -1);
        });
    }

    @Transactional
    public void approve(UUID moderatorId, String slug, UUID requestId) {
        CommunityJoinRequest request = pendingFor(moderatorId, slug, requestId);
        request.approve(moderatorId);
        admit(request.getCommunityId(), request.getUserId());
    }

    @Transactional
    public void decline(UUID moderatorId, String slug, UUID requestId) {
        pendingFor(moderatorId, slug, requestId).decline(moderatorId);
    }

    private CommunityJoinRequest pendingFor(UUID moderatorId, String slug, UUID requestId) {
        Community community = access.live(slug);
        access.moderator(community.getId(), moderatorId);
        return requestRepository.findByIdAndCommunityId(requestId, community.getId())
                .filter(CommunityJoinRequest::isPending)
                .orElseThrow(ApiException::notFound);
    }

    private CommunityJoinRequest pendingRequest(UUID communityId, UUID userId) {
        return requestRepository.findByCommunityIdAndUserIdAndStatus(communityId, userId, JoinRequestStatus.PENDING)
                .orElse(null);
    }

    private void admit(UUID communityId, UUID userId) {
        memberRepository.save(CommunityMember.member(communityId, userId));
        communityRepository.adjustMemberCount(communityId, 1);
    }
}
