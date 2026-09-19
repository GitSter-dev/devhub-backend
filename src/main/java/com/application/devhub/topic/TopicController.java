package com.application.devhub.topic;

import com.application.devhub.common.api.ApiEnvelope;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TopicController implements TopicApi {

    private final TopicRepository topicRepository;
    private final UserTopics userTopics;

    @Override
    @GetMapping("/topics")
    public ApiEnvelope<List<TopicResponse>> topics() {
        return ApiEnvelope.ok(topicRepository.findAll(Sort.by("name")).stream().map(TopicResponse::from).toList());
    }

    @Override
    @GetMapping("/users/me/topics")
    public ApiEnvelope<MyTopicsResponse> myTopics(JwtAuthenticationToken authentication) {
        return ApiEnvelope.ok(new MyTopicsResponse(userTopics.of(userIdOf(authentication))));
    }

    @Override
    @PutMapping("/users/me/topics")
    public ApiEnvelope<MyTopicsResponse> replaceMyTopics(JwtAuthenticationToken authentication,
                                                         @Valid @RequestBody MyTopicsRequest request) {
        return ApiEnvelope.ok(new MyTopicsResponse(userTopics.replace(userIdOf(authentication), request.slugs())));
    }

    private static UUID userIdOf(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getName());
    }
}
