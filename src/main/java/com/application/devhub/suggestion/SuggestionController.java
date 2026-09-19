package com.application.devhub.suggestion;

import com.application.devhub.common.api.ApiEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SuggestionController implements SuggestionApi {

    private static final int MAX_LIMIT = 50;

    private final SuggestionQuery suggestionQuery;

    @Override
    @GetMapping("/users/me/suggestions")
    public ApiEnvelope<List<SuggestionResponse>> suggestions(JwtAuthenticationToken authentication,
                                                             @RequestParam(defaultValue = "20") int limit) {
        int bounded = Math.clamp(limit, 1, MAX_LIMIT);
        return ApiEnvelope.ok(suggestionQuery.forUser(UUID.fromString(authentication.getName()), bounded));
    }
}
