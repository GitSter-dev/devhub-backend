package com.application.devhub.community;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("devhub.communities")
public record CommunityProperties(Duration minAccountAge, int maxOwned) {
}
