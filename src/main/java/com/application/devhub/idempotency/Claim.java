package com.application.devhub.idempotency;

import java.util.Optional;

public record Claim(Optional<IdempotencyRecord> existing) {

    public static Claim granted() {
        return new Claim(Optional.empty());
    }

    public static Claim heldBy(IdempotencyRecord record) {
        return new Claim(Optional.of(record));
    }

    public boolean isGranted() {
        return existing.isEmpty();
    }
}
