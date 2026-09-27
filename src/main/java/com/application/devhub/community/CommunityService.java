package com.application.devhub.community;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.common.metrics.DevHubMetrics;
import com.application.devhub.user.User;
import com.application.devhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;
    private final CommunityAccess access;
    private final UserRepository userRepository;
    private final CommunityProperties properties;
    private final DevHubMetrics metrics;

    @Transactional
    public Community create(UUID founderId, CreateCommunityRequest request) {
        ensureCanFound(founderId);
        if (communityRepository.existsBySlug(request.slug())) {
            throw ApiException.of(ErrorCode.COMMUNITY_SLUG_TAKEN);
        }
        Community community = communityRepository.saveAndFlush(
                Community.found(founderId, request.slug(), request.details()));
        memberRepository.save(CommunityMember.owner(community.getId(), founderId));
        communityRepository.adjustMemberCount(community.getId(), 1);
        metrics.communityCreated();
        return community;
    }

    @Transactional
    public void update(UUID moderatorId, String slug, UpdateCommunityRequest request) {
        Community community = access.live(slug);
        access.moderator(community.getId(), moderatorId);
        community.update(request.appliedTo(community));
    }

    private void ensureCanFound(UUID founderId) {
        User founder = userRepository.findById(founderId).orElseThrow(ApiException::unauthorized);
        if (founder.getCreatedAt().isAfter(Instant.now().minus(properties.minAccountAge()))) {
            throw ApiException.of(ErrorCode.ACCOUNT_TOO_NEW);
        }
        if (memberRepository.countByKeyUserIdAndRole(founderId, CommunityRole.OWNER) >= properties.maxOwned()) {
            throw ApiException.of(ErrorCode.COMMUNITY_LIMIT_REACHED);
        }
    }
}
