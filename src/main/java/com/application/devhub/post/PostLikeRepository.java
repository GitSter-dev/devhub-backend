package com.application.devhub.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLike.Key> {

    @Modifying
    @Query(value = """
            INSERT INTO post_likes (post_id, user_id) VALUES (:postId, :userId)
            ON CONFLICT (post_id, user_id) DO NOTHING
            """, nativeQuery = true)
    int like(@Param("postId") UUID postId, @Param("userId") UUID userId);

    @Modifying
    @Query("delete from PostLike l where l.key.postId = :postId and l.key.userId = :userId")
    int unlike(@Param("postId") UUID postId, @Param("userId") UUID userId);
}
