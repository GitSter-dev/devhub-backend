package com.application.devhub.otp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("devhub.codes")
public record OneTimeCodeProperties(CodePolicy emailVerification, CodePolicy passwordReset) {

    public CodePolicy policyFor(OneTimeCodePurpose purpose) {
        return switch (purpose) {
            case EMAIL_VERIFICATION -> emailVerification;
            case PASSWORD_RESET -> passwordReset;
        };
    }
}
