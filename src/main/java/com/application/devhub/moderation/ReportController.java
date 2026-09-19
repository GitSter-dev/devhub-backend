package com.application.devhub.moderation;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.ratelimit.RateLimitPolicy;
import com.application.devhub.ratelimit.RateLimitScope;
import com.application.devhub.ratelimit.RateLimited;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController implements ReportApi {

    private final ReportService reportService;

    @Override
    @PostMapping
    @RateLimited(value = RateLimitPolicy.REPORTING, scope = RateLimitScope.USER)
    public ApiEnvelope<Void> report(JwtAuthenticationToken authentication, @Valid @RequestBody ReportRequest request) {
        reportService.report(UUID.fromString(authentication.getName()), request);
        return ApiEnvelope.ok();
    }
}
