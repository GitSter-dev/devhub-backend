package com.application.devhub.moderation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ModerationCaseRepository extends JpaRepository<ModerationCase, UUID> {

    @Modifying
    @Query(value = """
            DELETE FROM moderation_cases c
            WHERE c.status <> 'OPEN'
              AND NOT EXISTS (SELECT 1 FROM reports r WHERE r.case_id = c.id)
              AND NOT EXISTS (SELECT 1 FROM moderation_actions a WHERE a.case_id = c.id)
            """, nativeQuery = true)
    int deleteEmptyResolvedCases();

    Optional<ModerationCase> findByTargetTypeAndTargetId(ReportTarget targetType, UUID targetId);

    @Query("select c from ModerationCase c where c.targetType = 'USER' and c.ownerId = :userId")
    Optional<ModerationCase> findUserCase(@Param("userId") UUID userId);
}
