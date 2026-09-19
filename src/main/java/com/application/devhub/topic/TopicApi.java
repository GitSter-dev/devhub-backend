package com.application.devhub.topic;

import com.application.devhub.common.api.ApiEnvelope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

@Tag(name = "Topics", description = "The tech stack catalog and the topics a developer follows")
public interface TopicApi {

    @Operation(summary = "List all topics",
            description = "The catalog a developer picks their stack from, sorted by name.")
    @ApiResponse(responseCode = "200", description = "Every topic", useReturnTypeSchema = true)
    ApiEnvelope<List<TopicResponse>> topics();

    @Operation(summary = "Get the signed-in user's topics",
            description = "The slugs the user picked as their stack, sorted. Empty until they finish that setup step.")
    @ApiResponse(responseCode = "200", description = "The user's topic slugs, sorted", useReturnTypeSchema = true)
    ApiEnvelope<MyTopicsResponse> myTopics(@Parameter(hidden = true) JwtAuthenticationToken authentication);

    @Operation(summary = "Replace the signed-in user's topics",
            description = "Sets the user's stack to exactly these topics (1 to 10, duplicates ignored). "
                    + "VALIDATION_FAILED with a `slugs` field error when the list is empty, too long or has an "
                    + "unknown slug. Safe to repeat.")
    @ApiResponse(responseCode = "200", description = "The user's topics after the change", useReturnTypeSchema = true)
    ApiEnvelope<MyTopicsResponse> replaceMyTopics(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                                  MyTopicsRequest request);
}
