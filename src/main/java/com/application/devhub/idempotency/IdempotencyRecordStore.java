package com.application.devhub.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class IdempotencyRecordStore {

    private final IdempotencyRecordRepository repository;
    private final IdempotencyProperties properties;

    @Transactional
    public Claim claim(String key, String endpoint, String requestHash) {
        if (tryInsert(key, endpoint, requestHash)) {
            return Claim.granted();
        }
        IdempotencyRecord existing = repository.findByIdempotencyKeyAndEndpoint(key, endpoint).orElse(null);
        if (existing == null || existing.isExpired()) {
            repository.deleteByKeyAndEndpoint(key, endpoint);
            return tryInsert(key, endpoint, requestHash) ? Claim.granted() : claim(key, endpoint, requestHash);
        }
        return Claim.heldBy(existing);
    }

    @Transactional
    public void complete(String key, String endpoint, int responseStatus, String responseBody) {
        repository.findByIdempotencyKeyAndEndpoint(key, endpoint)
                .ifPresent(record -> record.complete(responseStatus, responseBody));
    }

    @Transactional
    public void release(String key, String endpoint) {
        repository.deleteByKeyAndEndpoint(key, endpoint);
    }

    private boolean tryInsert(String key, String endpoint, String requestHash) {
        return repository.insertIfAbsent(key, endpoint, requestHash, Instant.now().plus(properties.ttl())) == 1;
    }
}
