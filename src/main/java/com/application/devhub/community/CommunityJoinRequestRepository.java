package com.application.devhub.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CommunityJoinRequestRepository extends JpaRepository<CommunityJoinRequest, UUID> {

    Optional<CommunityJoinRequest> findByCommunityIdAndUserIdAndStatus(UUID communityId, UUID userId,
                                                                      JoinRequestStatus status);

    Optional<CommunityJoinRequest> findByIdAndCommunityId(UUID id, UUID communityId);

    @Modifying
    @Query("DELETE FROM CommunityJoinRequest r WHERE r.userId = :userId AND r.status = com.application.devhub.community.JoinRequestStatus.PENDING")
    void deletePendingOf(@Param("userId") UUID userId);
}
