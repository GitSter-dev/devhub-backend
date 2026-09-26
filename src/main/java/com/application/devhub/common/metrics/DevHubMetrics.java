package com.application.devhub.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * The application's own meters, next to what Spring already records (HTTP, JVM, connection
 * pool). Names and tag values live here so dashboards have one place to look them up, and
 * every tag is a small fixed set: no user ids, no free text.
 */
@Component
public class DevHubMetrics {

    private final MeterRegistry registry;

    public DevHubMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** Open WebSocket connections, identified or not. */
    public void realtimeConnections(Supplier<Number> count) {
        Gauge.builder("devhub.realtime.connections", count)
                .description("Open realtime WebSocket connections")
                .register(registry);
    }

    /** One outbox event processed; outcome is sent, retrying or failed. */
    public void outboxEvent(Enum<?> type, String outcome) {
        Counter.builder("devhub.outbox.events")
                .description("Outbox events processed by the relay")
                .tag("type", tag(type))
                .tag("outcome", outcome)
                .register(registry)
                .increment();
    }

    /** One push delivery attempt to a single device. */
    public void pushDelivery(Enum<?> outcome) {
        Counter.builder("devhub.push.deliveries")
                .description("Push notification deliveries by outcome")
                .tag("outcome", tag(outcome))
                .register(registry)
                .increment();
    }

    /** A request turned away by a rate-limit policy. */
    public void rateLimitRejection(Enum<?> policy) {
        Counter.builder("devhub.ratelimit.rejections")
                .description("Requests rejected by a rate-limit policy")
                .tag("policy", tag(policy))
                .register(registry)
                .increment();
    }

    /** A request from an app build older than the platform's minimum. */
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
