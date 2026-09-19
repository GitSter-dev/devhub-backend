package com.application.devhub.block;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface BlockRepository extends JpaRepository<Block, Block.Key> {

    @Modifying
    @Query(value = """
            INSERT INTO blocks (blocker_id, blocked_id) VALUES (:blockerId, :blockedId)
            ON CONFLICT (blocker_id, blocked_id) DO NOTHING
            """, nativeQuery = true)
    int block(@Param("blockerId") UUID blockerId, @Param("blockedId") UUID blockedId);

    @Modifying
    @Query("delete from Block b where b.key.blockerId = :blockerId and b.key.blockedId = :blockedId")
    int unblock(@Param("blockerId") UUID blockerId, @Param("blockedId") UUID blockedId);

    @Query("""
            select count(b) > 0 from Block b
            where (b.key.blockerId = :first and b.key.blockedId = :second)
               or (b.key.blockerId = :second and b.key.blockedId = :first)
            """)
    boolean existsBetween(@Param("first") UUID first, @Param("second") UUID second);
}
