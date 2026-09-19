package com.application.devhub.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByInstallationId(String installationId);

    Optional<Device> findByPushToken(String pushToken);

    @Query("select d from Device d where d.userId = :userId and d.revokedAt is null and d.pushToken is not null")
    List<Device> findActiveByUserId(@Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Device d set d.revokedAt = :now, d.revocationReason = :reason
            where d.userId = :userId and d.revokedAt is null
            """)
    void revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now,
                          @Param("reason") DeviceRevocationReason reason);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Device d set d.revokedAt = :now, d.revocationReason = :reason
            where d.sessionFamilyId = :familyId and d.revokedAt is null
            """)
    void revokeAllForSession(@Param("familyId") UUID familyId, @Param("now") Instant now,
                             @Param("reason") DeviceRevocationReason reason);
}
