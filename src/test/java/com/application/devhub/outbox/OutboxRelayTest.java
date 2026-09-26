package com.application.devhub.outbox;

import com.application.devhub.common.metrics.DevHubMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxRelayTest {

    private static final OutboxProperties PROPERTIES =
            new OutboxProperties(Duration.ofSeconds(5), 20, 3, Duration.ofSeconds(30), Duration.ofDays(7), Duration.ofDays(30));

    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();

    @Test
    void failureSchedulesRetryWithoutBlockingTheBatch() {
        OutboxEvent failing = new OutboxEvent(OutboxEventType.EMAIL_VERIFICATION, "{\"fail\":true}");
        OutboxEvent healthy = new OutboxEvent(OutboxEventType.EMAIL_VERIFICATION, "{}");
        when(repository.claimDue(PROPERTIES.batchSize())).thenReturn(List.of(failing, healthy));

        relayWith(new FailingOnMarkerHandler()).relayPending();

        assertThat(failing.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(failing.getAttempts()).isEqualTo(1);
        assertThat(failing.getLastError()).isEqualTo("SMTP unavailable");
        assertThat(failing.getNextAttemptAt()).isAfter(Instant.now().plus(Duration.ofSeconds(25)));
        assertThat(healthy.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(processed("sent")).isEqualTo(1);
        assertThat(processed("retrying")).isEqualTo(1);
    }

    @Test
    void eventFailsPermanentlyAfterMaxAttempts() {
        OutboxEvent failing = new OutboxEvent(OutboxEventType.EMAIL_VERIFICATION, "{\"fail\":true}");
        when(repository.claimDue(PROPERTIES.batchSize())).thenReturn(List.of(failing));
        OutboxRelay relay = relayWith(new FailingOnMarkerHandler());

        for (int i = 0; i < PROPERTIES.maxAttempts(); i++) {
            relay.relayPending();
        }

        assertThat(failing.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(failing.getAttempts()).isEqualTo(PROPERTIES.maxAttempts());
        assertThat(processed("retrying")).isEqualTo(PROPERTIES.maxAttempts() - 1);
        assertThat(processed("failed")).isEqualTo(1);
    }

    private OutboxRelay relayWith(OutboxEventHandler handler) {
        return new OutboxRelay(repository, PROPERTIES, List.of(handler), new DevHubMetrics(meters));
    }

    private double processed(String outcome) {
        var counter = meters.find("devhub.outbox.events").tag("type", "email_verification").tag("outcome", outcome).counter();
        return counter == null ? 0 : counter.count();
    }

    private static class FailingOnMarkerHandler implements OutboxEventHandler {

        @Override
        public OutboxEventType type() {
            return OutboxEventType.EMAIL_VERIFICATION;
        }

        @Override
        public void handle(OutboxEvent event) {
            if (event.getPayload().contains("fail")) {
                throw new IllegalStateException("SMTP unavailable");
            }
        }
    }
}
