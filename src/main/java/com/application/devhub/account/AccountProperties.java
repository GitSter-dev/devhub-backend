package com.application.devhub.account;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("devhub.account")
public record AccountProperties(Duration gracePeriod, Duration usernameHold, int purgeBatchSize) {
}
