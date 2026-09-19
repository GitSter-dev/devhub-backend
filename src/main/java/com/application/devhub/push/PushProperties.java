package com.application.devhub.push;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;

@ConfigurationProperties("devhub.push")
public record PushProperties(boolean enabled, String credentials, Locale locale) {
}
