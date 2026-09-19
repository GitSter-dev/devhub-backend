package com.application.devhub.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ConfigurationProperties("devhub.rate-limit")
public record RateLimitProperties(boolean enabled, Map<RateLimitPolicy, Limit> limits) {

    public RateLimitProperties {
        List<RateLimitPolicy> missing = Arrays.stream(RateLimitPolicy.values())
                .filter(policy -> limits == null || !limits.containsKey(policy))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing devhub.rate-limit.limits for " + missing);
        }
    }

    public Limit limitFor(RateLimitPolicy policy) {
        return limits.get(policy);
    }
}
