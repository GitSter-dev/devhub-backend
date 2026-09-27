package com.application.devhub.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CommunityRuleRepository extends JpaRepository<CommunityRule, UUID> {

    List<CommunityRule> findByCommunityIdOrderByPosition(UUID communityId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM CommunityRule r WHERE r.communityId = :communityId")
    void deleteAllOf(@Param("communityId") UUID communityId);
}
