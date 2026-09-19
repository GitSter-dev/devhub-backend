package com.application.devhub.session;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Optional<RefreshToken> findFirstByParentIdOrderByCreatedAtDesc(UUID parentId);

    @Modifying
    @Query("""
            update RefreshToken t set t.revokedAt = :now, t.revocationReason = :reason
            where t.familyId = :familyId and t.revokedAt is null
            """)
    void revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now,
                      @Param("reason") RevocationReason reason);

    @Modifying
    @Query("""
            update RefreshToken t set t.revokedAt = :now, t.revocationReason = :reason
            where t.userId = :userId and t.revokedAt is null
            """)
    void revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now,
                          @Param("reason") RevocationReason reason);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :now")
    int deleteExpiredBefore(@Param("now") Instant now);

    @Query("""
            select count(t) > 0 from RefreshToken t
            where t.familyId = :familyId and t.revokedAt is null and t.expiresAt > :now
            """)
    boolean existsActiveInFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);
}
