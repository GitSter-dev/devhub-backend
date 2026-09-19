package com.application.devhub.device;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.idempotency.Idempotent;
import com.application.devhub.ratelimit.RateLimitPolicy;
import com.application.devhub.ratelimit.RateLimited;
import com.application.devhub.security.JwtConfig;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceController implements DeviceApi {

    private final DeviceRegistry registry;
    private final TestNotificationService testNotificationService;

    @Override
    @PutMapping("/current")
    public ApiEnvelope<DeviceResponse> register(JwtAuthenticationToken authentication,
                                                @Valid @RequestBody RegisterDeviceRequest request) {
        DeviceRegistration registration = new DeviceRegistration(userIdOf(authentication), sessionIdOf(authentication),
                request.installationId(), request.platform(), request.pushToken(), request.deviceName(),
                request.appVersion());
        return ApiEnvelope.ok(DeviceResponse.from(registry.register(registration)));
    }

    @Override
    @PostMapping("/current/test-notification")
    @Idempotent
    @RateLimited(RateLimitPolicy.TEST_NOTIFICATION)
    public ResponseEntity<ApiEnvelope<Void>> sendTestNotification(JwtAuthenticationToken authentication) {
        testNotificationService.request(userIdOf(authentication));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiEnvelope.ok());
    }

    private static UUID userIdOf(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getName());
    }

    private static UUID sessionIdOf(JwtAuthenticationToken authentication) {
        String sessionId = authentication.getToken().getClaimAsString(JwtConfig.SESSION_CLAIM);
        return sessionId == null ? null : UUID.fromString(sessionId);
    }
}
