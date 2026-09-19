package com.application.devhub.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class EmailStyleLoader {

    private static final String COMMON_STYLESHEET = "classpath:stylesheets/email/common.css";

    private final ResourceLoader resourceLoader;
    private final Map<EmailTemplate, String> cache = new ConcurrentHashMap<>();

    public String stylesFor(EmailTemplate template) {
        return cache.computeIfAbsent(template, this::load);
    }

    private String load(EmailTemplate template) {
        String common = read(resourceLoader.getResource(COMMON_STYLESHEET));
        Resource specific = resourceLoader.getResource("classpath:" + template.stylesheetPath());
        return specific.exists() ? common + "\n" + read(specific) : common;
    }

    private String read(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read stylesheet " + resource.getDescription(), e);
        }
    }
}
