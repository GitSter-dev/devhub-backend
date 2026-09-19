package com.application.devhub.follow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, Follow.Key> {

    @Modifying
    @Query(value = """
            INSERT INTO follows (follower_id, followee_id) VALUES (:followerId, :followeeId)
            ON CONFLICT (follower_id, followee_id) DO NOTHING
            """, nativeQuery = true)
    void follow(@Param("followerId") UUID followerId, @Param("followeeId") UUID followeeId);

    @Modifying
    @Query("delete from Follow f where f.key.followerId = :followerId and f.key.followeeId = :followeeId")
    void unfollow(@Param("followerId") UUID followerId, @Param("followeeId") UUID followeeId);
}
