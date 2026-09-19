package com.application.devhub.topic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserTopicRepository extends JpaRepository<UserTopic, UserTopic.Key> {

    @Query("select t.key.topicSlug from UserTopic t where t.key.userId = :userId order by t.key.topicSlug")
    List<String> findSlugsByUserId(@Param("userId") UUID userId);

    @Query("select count(t) > 0 from UserTopic t where t.key.userId = :userId")
    boolean existsForUser(@Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserTopic t where t.key.userId = :userId")
    void deleteAllForUser(@Param("userId") UUID userId);
}
