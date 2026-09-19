package com.application.devhub.device;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.openapi.ApiErrors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static com.application.devhub.common.api.ErrorCode.SESSION_ENDED;

@Tag(name = "Devices", description = "App installs that can receive push notifications")
public interface DeviceApi {

    @Operation(summary = "Register this app install for push notifications",
            description = "Creates or updates the device identified by installationId and binds it to the current "
                    + "session. Safe to call on every app start and whenever the FCM token rotates. When the session "
                    + "ends (logout, another login, password reset) the device stops receiving pushes automatically. "
                    + "An install used by another account moves to that account; a push token seen on a new install "
                    + "is released from the old one.")
    @ApiResponse(responseCode = "200", description = "Device registered", useReturnTypeSchema = true)
    @ApiErrors(SESSION_ENDED)
    ApiEnvelope<DeviceResponse> register(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                         RegisterDeviceRequest request);

    @Operation(summary = "Send a test notification",
            description = "Queues a test push to every active device of the signed-in user. Delivery is asynchronous.")
    @ApiResponse(responseCode = "202", description = "Test notification queued", useReturnTypeSchema = true)
    ResponseEntity<ApiEnvelope<Void>> sendTestNotification(@Parameter(hidden = true) JwtAuthenticationToken authentication);
}
