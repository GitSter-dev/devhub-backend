package com.application.devhub.moderation;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.moderation.ModerationViews.AuditPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/moderation/actions")
@RequiredArgsConstructor
public class AuditLogController implements AuditLogApi {

    private final AuditLogQuery auditLogQuery;

    @Override
    @GetMapping
    public ApiEnvelope<AuditPage> entries(@RequestParam(required = false) UUID userId,
                                          @RequestParam(required = false) String cursor) {
        return ApiEnvelope.ok(auditLogQuery.entries(userId, cursor));
    }
}
