package com.application.devhub.follow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, Follow.Key> {

    @Modifying
    @Query(value = """
            INSERT INTO follows (follower_id, followee_id) VALUES (:followerId, :followeeId)
            ON CONFLICT (follower_id, followee_id) DO NOTHING
            """, nativeQuery = true)
    int follow(@Param("followerId") UUID followerId, @Param("followeeId") UUID followeeId);

    @Modifying
    @Query("delete from Follow f where f.key.followerId = :followerId and f.key.followeeId = :followeeId")
    int unfollow(@Param("followerId") UUID followerId, @Param("followeeId") UUID followeeId);

    @Query(value = """
            SELECT follower_id FROM follows
            WHERE followee_id = :followeeId AND follower_id > :after
            ORDER BY follower_id
            LIMIT :limit
            """, nativeQuery = true)
    List<UUID> followerIdsAfter(@Param("followeeId") UUID followeeId, @Param("after") UUID after,
                                @Param("limit") int limit);

    @Query("select count(f) from Follow f where f.key.followeeId = :userId")
    long countFollowers(@Param("userId") UUID userId);

    @Query("select count(f) from Follow f where f.key.followerId = :userId")
    long countFollowing(@Param("userId") UUID userId);

    @Query("select count(f) > 0 from Follow f where f.key.followerId = :followerId and f.key.followeeId = :followeeId")
    boolean isFollowing(@Param("followerId") UUID followerId, @Param("followeeId") UUID followeeId);
}
