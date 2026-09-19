package com.application.devhub.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Modifying
    @Query(value = """
            WITH upserted AS (
                INSERT INTO notifications (id, recipient_id, type, group_key, subject_id, push_due_at, created_at, updated_at)
                SELECT gen_random_uuid(), u.id, :type, :groupKey, :subjectId, now() + make_interval(secs => :burstSeconds),
                       clock_timestamp(), clock_timestamp()
                FROM users u
                WHERE u.id IN (:recipientIds)
                ON CONFLICT (recipient_id, group_key) WHERE seen_at IS NULL DO UPDATE SET
                    updated_at = clock_timestamp(),
                    push_due_at = COALESCE(notifications.push_due_at,
                        CASE WHEN notifications.pushed_count < :individualPushes
                             THEN now() + make_interval(secs => :burstSeconds)
                             ELSE GREATEST(now(), notifications.pushed_at + make_interval(secs => :digestSeconds))
                        END)
                RETURNING id
            )
            INSERT INTO notification_actors (notification_id, actor_id, subject_id, acted_at)
            SELECT id, :actorId, :actorSubjectId, clock_timestamp() FROM upserted
            ON CONFLICT (notification_id, actor_id) DO UPDATE SET subject_id = excluded.subject_id, acted_at = excluded.acted_at
            """, nativeQuery = true)
    int record(@Param("recipientIds") Collection<UUID> recipientIds,
               @Param("type") String type,
               @Param("groupKey") String groupKey,
               @Param("subjectId") UUID subjectId,
               @Param("actorId") UUID actorId,
               @Param("actorSubjectId") UUID actorSubjectId,
               @Param("burstSeconds") double burstSeconds,
               @Param("digestSeconds") double digestSeconds,
               @Param("individualPushes") int individualPushes);

    @Modifying
    @Query(value = """
            DELETE FROM notification_actors a
            USING notifications n
            WHERE a.notification_id = n.id
              AND n.recipient_id = :recipientId
              AND n.group_key = :groupKey
              AND a.actor_id = :actorId
            """, nativeQuery = true)
    int withdraw(@Param("recipientId") UUID recipientId, @Param("groupKey") String groupKey,
                 @Param("actorId") UUID actorId);

    @Modifying
    @Query(value = "DELETE FROM notification_actors WHERE subject_id = :subjectId", nativeQuery = true)
    int withdrawSubject(@Param("subjectId") UUID subjectId);

    @Query(value = """
            SELECT DISTINCT n.recipient_id FROM notifications n
            JOIN notification_actors a ON a.notification_id = n.id
            WHERE a.subject_id = :subjectId
            """, nativeQuery = true)
    List<UUID> recipientsOfSubject(@Param("subjectId") UUID subjectId);

    @Modifying
    @Query(value = """
            UPDATE notifications SET seen_at = now(), push_due_at = NULL
            WHERE recipient_id = :recipientId AND seen_at IS NULL AND updated_at <= :until
            """, nativeQuery = true)
    int markSeen(@Param("recipientId") UUID recipientId, @Param("until") Instant until);

    @Query(value = """
            SELECT * FROM notifications
            WHERE push_due_at <= now()
            ORDER BY push_due_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Notification> claimDue(@Param("limit") int limit);

    @Modifying
    @Query(value = "DELETE FROM notifications WHERE seen_at < :before", nativeQuery = true)
    int deleteSeenBefore(@Param("before") Instant before);

    @Modifying
    @Query(value = """
            DELETE FROM notifications n
            WHERE n.updated_at < :before
              AND NOT EXISTS (SELECT 1 FROM notification_actors a WHERE a.notification_id = n.id)
            """, nativeQuery = true)
    int deleteEmptyBefore(@Param("before") Instant before);
}
