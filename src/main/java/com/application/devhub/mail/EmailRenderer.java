package com.application.devhub.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class EmailRenderer {

    private static final String STYLES_VARIABLE = "styles";

    private final ITemplateEngine templateEngine;
    private final EmailStyleLoader styleLoader;
    private final MessageSource messageSource;
    private final MailProperties properties;

    public RenderedEmail render(EmailTemplate template, Map<String, Object> variables) {
        Context context = new Context(properties.locale(), variables);
        context.setVariable(STYLES_VARIABLE, styleLoader.stylesFor(template));
        String html = templateEngine.process(template.templatePath(), context);
        String subject = messageSource.getMessage(template.subjectKey(), null, properties.locale());
        return new RenderedEmail(subject, html);
    }
}
