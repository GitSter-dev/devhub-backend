package com.application.devhub.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = """
            SELECT * FROM outbox_events
            WHERE status = 'PENDING' AND next_attempt_at <= now()
            ORDER BY next_attempt_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> claimDue(@Param("limit") int limit);

    @Modifying
    @Query("delete from OutboxEvent e where e.status = :status and e.processedAt < :before")
    int deleteProcessedBefore(@Param("status") OutboxStatus status, @Param("before") Instant before);
}
