package com.application.devhub.post;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    long countByCommunityIdAndPinnedAtIsNotNull(UUID communityId);
}
