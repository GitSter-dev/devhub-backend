package com.application.devhub.community;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CommunityJoinRequestRepository extends JpaRepository<CommunityJoinRequest, UUID> {

    Optional<CommunityJoinRequest> findByCommunityIdAndUserIdAndStatus(UUID communityId, UUID userId,
                                                                      JoinRequestStatus status);

    Optional<CommunityJoinRequest> findByIdAndCommunityId(UUID id, UUID communityId);
}
