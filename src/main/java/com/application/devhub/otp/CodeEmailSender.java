package com.application.devhub.otp;

import com.application.devhub.mail.EmailRenderer;
import com.application.devhub.mail.EmailSender;
import com.application.devhub.mail.EmailTemplate;
import com.application.devhub.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CodeEmailSender {

    private final OneTimeCodeIssuer issuer;
    private final OneTimeCodeProperties properties;
    private final EmailRenderer renderer;
    private final EmailSender sender;

    public void send(User user, OneTimeCodePurpose purpose, EmailTemplate template) {
        if (!issuer.canIssue(user, purpose)) {
            return;
        }
        String code = issuer.issue(user, purpose);
        Map<String, Object> variables = Map.of(
                "displayName", user.getDisplayName(),
                "code", code,
                "expiresInMinutes", properties.policyFor(purpose).ttl().toMinutes());
        sender.send(user.getEmail(), renderer.render(template, variables));
    }
}
