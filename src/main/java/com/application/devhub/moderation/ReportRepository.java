package com.application.devhub.moderation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(UUID reporterId, ReportTarget targetType, UUID targetId);

    @Query("select r.reporterId from Report r where r.caseId = :caseId")
    List<UUID> reporterIdsOf(@Param("caseId") UUID caseId);

    @Query(value = """
            SELECT count(*) FROM reports r
            JOIN users u ON u.id = r.reporter_id
            WHERE r.case_id = :caseId
              AND u.email_verified_at IS NOT NULL
              AND u.created_at < now() - make_interval(days => :minAgeDays)
              AND NOT EXISTS (SELECT 1 FROM moderation_actions a
                              WHERE a.target_user_id = r.reporter_id
                                AND a.action <> 'DISMISS'
                                AND a.created_at > now() - make_interval(days => :recentActionDays))
            """, nativeQuery = true)
    long countEstablishedReporters(@Param("caseId") UUID caseId, @Param("minAgeDays") int minAgeDays,
                                   @Param("recentActionDays") int recentActionDays);
}
