package com.application.devhub.push;

import com.application.devhub.common.metrics.DevHubMetrics;
import com.application.devhub.device.Device;
import com.application.devhub.device.DeviceRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PushDispatcher {

    private final DeviceRegistry deviceRegistry;
    private final PushSender pushSender;
    private final DevHubMetrics metrics;

    public void dispatch(UUID userId, PushMessage message) {
        List<String> tokens = deviceRegistry.activeDevicesOf(userId).stream().map(Device::getPushToken).toList();
        if (tokens.isEmpty()) {
            return;
        }
        List<PushResult> results = pushSender.send(tokens, message);
        results.forEach(result -> metrics.pushDelivery(result.outcome()));
        results.stream()
                .filter(result -> result.outcome() == PushOutcome.TOKEN_INVALID)
                .forEach(result -> deviceRegistry.revokeInvalidToken(result.token()));
        if (results.stream().allMatch(result -> result.outcome() == PushOutcome.FAILED)) {
            throw new PushDeliveryException("Every push delivery failed; the outbox will retry");
        }
    }
}
