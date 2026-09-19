package com.application.devhub.account;

import com.application.devhub.user.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AccountPurgeRepository extends Repository<User, UUID> {

    @Modifying
    @Query(value = """
            UPDATE posts SET deleted_at = COALESCE(deleted_at, now()), body = NULL, code = NULL, code_language = NULL
            WHERE author_id = :userId
            """, nativeQuery = true)
    int erasePosts(@Param("userId") UUID userId);

    @Modifying
    @Query(value = """
            UPDATE messages SET deleted_at = COALESCE(deleted_at, now()), body = NULL, code = NULL, code_language = NULL
            WHERE sender_id = :userId
            """, nativeQuery = true)
    int eraseMessages(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "DELETE FROM post_likes WHERE user_id = :userId", nativeQuery = true)
    int deleteLikes(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "DELETE FROM follows WHERE follower_id = :userId OR followee_id = :userId", nativeQuery = true)
    int deleteFollows(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "DELETE FROM blocks WHERE blocker_id = :userId OR blocked_id = :userId", nativeQuery = true)
    int deleteBlocks(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "DELETE FROM user_topics WHERE user_id = :userId", nativeQuery = true)
    int deleteTopics(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "DELETE FROM devices WHERE user_id = :userId", nativeQuery = true)
    int deleteDevices(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "DELETE FROM refresh_tokens WHERE user_id = :userId", nativeQuery = true)
    int deleteRefreshTokens(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "DELETE FROM notification_actors WHERE actor_id = :userId", nativeQuery = true)
    int deleteNotificationActors(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "DELETE FROM notifications WHERE recipient_id = :userId", nativeQuery = true)
    int deleteNotifications(@Param("userId") UUID userId);
}
