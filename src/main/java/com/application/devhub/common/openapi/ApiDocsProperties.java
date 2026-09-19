package com.application.devhub.common.openapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("devhub.api-docs")
public record ApiDocsProperties(String title, String version, String description) {
}
