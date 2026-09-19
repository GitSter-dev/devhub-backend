package com.application.devhub.ratelimit;

import java.time.Duration;

public record Limit(int capacity, Duration period) {
}
