package com.application.devhub.topic;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class KnownTopicsValidator implements ConstraintValidator<KnownTopics, List<String>> {

    private final TopicRepository topicRepository;

    @Override
    public boolean isValid(List<String> slugs, ConstraintValidatorContext context) {
        return slugs == null || slugs.isEmpty() || topicRepository.countBySlugIn(slugs) == slugs.size();
    }
}
