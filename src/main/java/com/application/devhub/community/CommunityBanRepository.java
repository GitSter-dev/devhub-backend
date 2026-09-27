package com.application.devhub.community;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityBanRepository extends JpaRepository<CommunityBan, CommunityMember.Key> {
}
