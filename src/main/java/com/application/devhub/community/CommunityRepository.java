package com.application.devhub.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CommunityRepository extends JpaRepository<Community, UUID> {

    Optional<Community> findBySlugAndRemovedAtIsNull(String slug);

    boolean existsBySlug(String slug);

    @Modifying
    @Query(value = "UPDATE communities SET member_count = member_count + :delta WHERE id = :id", nativeQuery = true)
    void adjustMemberCount(@Param("id") UUID id, @Param("delta") int delta);
}
