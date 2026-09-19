package com.application.devhub.realtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties("devhub.realtime")
public record RealtimeProperties(Duration heartbeat, Duration timeToFirstMessage, List<String> allowedOrigins) {
}
