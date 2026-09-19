package com.application.devhub.moderation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ModerationCaseRepository extends JpaRepository<ModerationCase, UUID> {

    Optional<ModerationCase> findByTargetTypeAndTargetId(ReportTarget targetType, UUID targetId);

    @Query("select c from ModerationCase c where c.targetType = 'USER' and c.ownerId = :userId")
    Optional<ModerationCase> findUserCase(@Param("userId") UUID userId);
}
