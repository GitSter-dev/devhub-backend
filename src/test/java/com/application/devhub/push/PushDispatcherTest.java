package com.application.devhub.push;

import com.application.devhub.device.Device;
import com.application.devhub.device.DeviceRegistration;
import com.application.devhub.device.DeviceRegistry;
import com.application.devhub.device.Platform;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PushDispatcherTest {

    private static final UUID USER = UUID.randomUUID();
    private static final PushMessage MESSAGE = new PushMessage("Title", "Body", Map.of("kind", "test"));

    private final DeviceRegistry registry = mock(DeviceRegistry.class);
    private final PushSender sender = mock(PushSender.class);
    private final PushDispatcher dispatcher = new PushDispatcher(registry, sender);

    @Test
    void sendsTheMessageToEveryActiveDeviceOfTheUser() {
        when(registry.activeDevicesOf(USER)).thenReturn(List.of(device("token-a"), device("token-b")));
        when(sender.send(anyList(), any())).thenReturn(List.of(
                new PushResult("token-a", PushOutcome.DELIVERED),
                new PushResult("token-b", PushOutcome.DELIVERED)));

        dispatcher.dispatch(USER, MESSAGE);

        verify(sender).send(List.of("token-a", "token-b"), MESSAGE);
        verify(registry, never()).revokeInvalidToken(anyString());
    }

    @Test
    void invalidTokensAreRevokedAndDeliveredOnesKept() {
        when(registry.activeDevicesOf(USER)).thenReturn(List.of(device("token-ok"), device("token-dead")));
        when(sender.send(anyList(), any())).thenReturn(List.of(
                new PushResult("token-ok", PushOutcome.DELIVERED),
                new PushResult("token-dead", PushOutcome.TOKEN_INVALID)));

        dispatcher.dispatch(USER, MESSAGE);

        verify(registry).revokeInvalidToken("token-dead");
        verify(registry, never()).revokeInvalidToken("token-ok");
    }

    @Test
    void aUserWithoutActiveDevicesSendsNothing() {
        when(registry.activeDevicesOf(USER)).thenReturn(List.of());

        dispatcher.dispatch(USER, MESSAGE);

        verify(sender, never()).send(anyList(), any());
    }

    @Test
    void aBatchWhereEveryDeliveryFailedIsRetriedByTheOutbox() {
        when(registry.activeDevicesOf(USER)).thenReturn(List.of(device("token-ok")));
        when(sender.send(anyList(), any())).thenReturn(List.of(new PushResult("token-ok", PushOutcome.FAILED)));

        assertThatThrownBy(() -> dispatcher.dispatch(USER, MESSAGE)).isInstanceOf(PushDeliveryException.class);
    }

    @Test
    void aBatchOfOnlyDeadTokensIsPrunedWithoutRetrying() {
        when(registry.activeDevicesOf(USER)).thenReturn(List.of(device("token-dead")));
        when(sender.send(anyList(), any())).thenReturn(List.of(new PushResult("token-dead", PushOutcome.TOKEN_INVALID)));

        assertThatNoException().isThrownBy(() -> dispatcher.dispatch(USER, MESSAGE));
        verify(registry).revokeInvalidToken("token-dead");
    }

    private static Device device(String pushToken) {
        Device device = new Device("install-" + pushToken);
        device.register(new DeviceRegistration(USER, UUID.randomUUID(), "install-" + pushToken, Platform.ANDROID,
                pushToken, "Pixel 8", "1.0.0"));
        return device;
    }
}
