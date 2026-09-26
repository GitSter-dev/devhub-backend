package com.application.devhub.ratelimit;

import com.application.devhub.common.metrics.DevHubMetrics;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.EstimationProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

@Component
public class RateLimiter {

    private final ProxyManager<String> buckets;
    private final boolean enabled;
    private final DevHubMetrics metrics;
    private final Map<RateLimitPolicy, BucketConfiguration> configurations = new EnumMap<>(RateLimitPolicy.class);

    public RateLimiter(ProxyManager<String> buckets, RateLimitProperties properties, DevHubMetrics metrics) {
        this.buckets = buckets;
        this.metrics = metrics;
        this.enabled = properties.enabled();
        for (RateLimitPolicy policy : RateLimitPolicy.values()) {
            configurations.put(policy, configurationOf(properties.limitFor(policy)));
        }
    }

    public void consume(RateLimitPolicy policy, String key) {
        if (!enabled) {
            return;
        }
        ConsumptionProbe probe = bucket(policy, key).tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            metrics.rateLimitRejection(policy);
            throw new RateLimitExceededException(Duration.ofNanos(probe.getNanosToWaitForRefill()));
        }
    }

    public void ensureAvailable(RateLimitPolicy policy, String key) {
        if (!enabled) {
            return;
        }
        EstimationProbe probe = bucket(policy, key).estimateAbilityToConsume(1);
        if (!probe.canBeConsumed()) {
            metrics.rateLimitRejection(policy);
            throw new RateLimitExceededException(Duration.ofNanos(probe.getNanosToWaitForRefill()));
        }
    }

    public void recordHit(RateLimitPolicy policy, String key) {
        if (enabled) {
            bucket(policy, key).tryConsume(1);
        }
    }

    private Bucket bucket(RateLimitPolicy policy, String key) {
        return buckets.builder().build(policy.name() + ":" + key, () -> configurations.get(policy));
    }

    private static BucketConfiguration configurationOf(Limit limit) {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(limit.capacity())
                        .refillGreedy(limit.capacity(), limit.period())
                        .build())
                .build();
    }
}
