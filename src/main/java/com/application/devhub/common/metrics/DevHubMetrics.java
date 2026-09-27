package com.application.devhub.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.function.Supplier;

@Component
public class DevHubMetrics {

    private final MeterRegistry registry;

    public DevHubMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void realtimeConnections(Supplier<Number> count) {
        Gauge.builder("devhub.realtime.connections", count)
                .description("Open realtime WebSocket connections")
                .register(registry);
    }

    public void outboxEvent(Enum<?> type, String outcome) {
        Counter.builder("devhub.outbox.events")
                .description("Outbox events processed by the relay")
                .tag("type", tag(type))
                .tag("outcome", outcome)
                .register(registry)
                .increment();
    }

    public void pushDelivery(Enum<?> outcome) {
        Counter.builder("devhub.push.deliveries")
                .description("Push notification deliveries by outcome")
                .tag("outcome", tag(outcome))
                .register(registry)
                .increment();
    }

    public void rateLimitRejection(Enum<?> policy) {
        Counter.builder("devhub.ratelimit.rejections")
                .description("Requests rejected by a rate-limit policy")
                .tag("policy", tag(policy))
                .register(registry)
                .increment();
    }

    public void outdatedClientRejection(String platform, String version) {
        Counter.builder("devhub.clients.outdated.rejections")
                .description("Requests turned away because the app build is too old")
                .tag("platform", platform.toLowerCase(Locale.ROOT))
                .tag("version", version)
                .register(registry)
                .increment();
    }

    private static String tag(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
