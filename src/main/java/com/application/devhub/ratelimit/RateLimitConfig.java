package com.application.devhub.ratelimit;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.caffeine.Bucket4jCaffeine;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    private static final long MAX_TRACKED_BUCKETS = 100_000;

    @Bean
    ProxyManager<String> rateLimitBuckets() {
        return Bucket4jCaffeine.<String>builderFor(Caffeine.newBuilder().maximumSize(MAX_TRACKED_BUCKETS))
                .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ZERO))
                .build();
    }
}
