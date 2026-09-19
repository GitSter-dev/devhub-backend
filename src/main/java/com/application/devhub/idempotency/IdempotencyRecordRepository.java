package com.application.devhub.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO idempotency_records (id, idempotency_key, endpoint, request_hash, status, expires_at)
            VALUES (gen_random_uuid(), :key, :endpoint, :requestHash, 'IN_PROGRESS', :expiresAt)
            ON CONFLICT (idempotency_key, endpoint) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("key") String key, @Param("endpoint") String endpoint,
                       @Param("requestHash") String requestHash, @Param("expiresAt") Instant expiresAt);

    Optional<IdempotencyRecord> findByIdempotencyKeyAndEndpoint(String idempotencyKey, String endpoint);

    @Modifying
    @Query("delete from IdempotencyRecord r where r.idempotencyKey = :key and r.endpoint = :endpoint")
    void deleteByKeyAndEndpoint(@Param("key") String key, @Param("endpoint") String endpoint);

    @Modifying
    @Query("delete from IdempotencyRecord r where r.expiresAt < :now")
    int deleteExpiredBefore(@Param("now") Instant now);
}
