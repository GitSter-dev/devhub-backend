package com.application.devhub.topic;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserTopics {

    private final UserTopicRepository repository;

    @Transactional(readOnly = true)
    public List<String> of(UUID userId) {
        return repository.findSlugsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean hasAny(UUID userId) {
        return repository.existsForUser(userId);
    }

    @Transactional
    public List<String> replace(UUID userId, List<String> slugs) {
        repository.deleteAllForUser(userId);
        repository.saveAll(slugs.stream().map(slug -> UserTopic.of(userId, slug)).toList());
        return repository.findSlugsByUserId(userId);
    }
}
