package com.application.devhub.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface HeldUsernameRepository extends JpaRepository<HeldUsername, String> {

    @Query("select h from HeldUsername h where h.usernameLower = lower(:username) and h.heldUntil > :now")
    Optional<HeldUsername> findActive(@Param("username") String username, @Param("now") Instant now);

    @Modifying
    @Query(value = """
            INSERT INTO held_usernames (username_lower, user_id, held_until)
            VALUES (lower(:username), :userId, :heldUntil)
            ON CONFLICT (username_lower) DO UPDATE SET user_id = excluded.user_id, held_until = excluded.held_until
            """, nativeQuery = true)
    void hold(@Param("username") String username, @Param("userId") UUID userId, @Param("heldUntil") Instant heldUntil);

    @Modifying
    @Query("delete from HeldUsername h where h.usernameLower = lower(:username)")
    void release(@Param("username") String username);
}
