package com.application.devhub.community;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.api.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CommunityAccess {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;

    public Community live(String slug) {
        return communityRepository.findBySlugAndRemovedAtIsNull(slug).orElseThrow(ApiException::notFound);
    }

    public Community live(UUID communityId) {
        return communityRepository.findById(communityId).filter(community -> !community.isRemoved())
                .orElseThrow(ApiException::notFound);
    }

    public Optional<CommunityMember> membership(UUID communityId, UUID userId) {
        return memberRepository.findById(new CommunityMember.Key(communityId, userId));
    }

    public CommunityMember member(UUID communityId, UUID userId) {
        return membership(communityId, userId).orElseThrow(() -> ApiException.of(ErrorCode.NOT_COMMUNITY_MEMBER));
    }

    public CommunityMember moderator(UUID communityId, UUID userId) {
        return membership(communityId, userId).filter(CommunityMember::canModerate)
                .orElseThrow(() -> ApiException.of(ErrorCode.NOT_COMMUNITY_MODERATOR));
    }
}
