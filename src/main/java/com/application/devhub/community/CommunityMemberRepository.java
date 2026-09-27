package com.application.devhub.community;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommunityMemberRepository extends JpaRepository<CommunityMember, CommunityMember.Key> {

    long countByKeyUserIdAndRole(UUID userId, CommunityRole role);
}
