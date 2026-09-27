package com.application.devhub.moderation;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.moderation.ModerationViews.CaseDetail;
import com.application.devhub.moderation.ModerationViews.CasePage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/moderation/cases")
@RequiredArgsConstructor
public class ModerationController implements ModerationApi {

    private final ModerationService moderationService;
    private final ModerationQueries moderationQueries;

    @Override
    @GetMapping
    public ApiEnvelope<CasePage> cases(JwtAuthenticationToken authentication,
                                       @RequestParam(defaultValue = "OPEN") CaseStatus status,
                                       @RequestParam(required = false) UUID communityId,
                                       @RequestParam(required = false) String cursor) {
        return ApiEnvelope.ok(moderationQueries.cases(status, communityId, cursor));
    }

    @Override
    @GetMapping("/{caseId}")
    public ApiEnvelope<CaseDetail> caseDetail(JwtAuthenticationToken authentication, @PathVariable UUID caseId) {
        return ApiEnvelope.ok(moderationQueries.caseDetail(caseId));
    }

    @Override
    @PostMapping("/{caseId}/actions")
    public ApiEnvelope<CaseDetail> act(JwtAuthenticationToken authentication, @PathVariable UUID caseId,
                                       @Valid @RequestBody ModerationActionRequest request) {
        moderationService.act(UUID.fromString(authentication.getName()), caseId, request);
        return ApiEnvelope.ok(moderationQueries.caseDetail(caseId));
    }
}
