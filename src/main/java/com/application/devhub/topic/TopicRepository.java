package com.application.devhub.topic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface TopicRepository extends JpaRepository<Topic, String> {

    long countBySlugIn(Collection<String> slugs);
}
