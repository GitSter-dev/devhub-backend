package com.application.devhub.common.api;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ErrorMessageResolver {

    private static final String KEY_PREFIX = "error.";

    private final MessageSource messageSource;

    public String resolve(ErrorCode code, Object... args) {
        return messageSource.getMessage(KEY_PREFIX + code.name(), args, code.name(), LocaleContextHolder.getLocale());
    }
}
