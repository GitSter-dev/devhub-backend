package com.application.devhub.otp;

import java.time.Duration;

public record CodePolicy(Duration ttl, int maxAttempts, Duration resendCooldown, int dailyLimit) {
}
