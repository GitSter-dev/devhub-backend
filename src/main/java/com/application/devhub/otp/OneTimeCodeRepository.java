package com.application.devhub.otp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OneTimeCodeRepository extends JpaRepository<OneTimeCode, UUID> {

    Optional<OneTimeCode> findByUserIdAndPurpose(UUID userId, OneTimeCodePurpose purpose);

    @Modifying
    @Query("delete from OneTimeCode c where c.expiresAt < :now and c.windowStartedAt < :windowCutoff")
    int deleteExpiredOutsideIssueWindow(@Param("now") Instant now, @Param("windowCutoff") Instant windowCutoff);
}
