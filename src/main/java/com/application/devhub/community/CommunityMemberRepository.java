package com.application.devhub.community;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.UUID;

public interface CommunityMemberRepository extends JpaRepository<CommunityMember, CommunityMember.Key> {

    long countByKeyUserIdAndRole(UUID userId, CommunityRole role);

    @Query("SELECT m.key.userId FROM CommunityMember m WHERE m.key.communityId = :communityId AND m.role <> "
            + "com.application.devhub.community.CommunityRole.MEMBER")
    List<UUID> moderatorIdsOf(@Param("communityId") UUID communityId);

    @Query("SELECT m FROM CommunityMember m WHERE m.key.userId = :userId")
    List<CommunityMember> membershipsOf(@Param("userId") UUID userId);

    @Query("SELECT m FROM CommunityMember m WHERE m.key.communityId = :communityId AND m.key.userId <> :leaving "
            + "ORDER BY CASE WHEN m.role = com.application.devhub.community.CommunityRole.MODERATOR THEN 0 ELSE 1 END, "
            + "m.joinedAt")
    List<CommunityMember> successorsOf(@Param("communityId") UUID communityId, @Param("leaving") UUID leaving,
                                       Limit limit);
}
