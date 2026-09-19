package com.application.devhub.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;

@ConfigurationProperties("devhub.mail")
public record MailProperties(String from, Locale locale) {
}
