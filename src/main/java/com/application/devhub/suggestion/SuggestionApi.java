package com.application.devhub.suggestion;

import com.application.devhub.common.api.ApiEnvelope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

@Tag(name = "Suggestions", description = "Developers the signed-in user might want to follow")
public interface SuggestionApi {

    @Operation(summary = "Suggest developers to follow",
            description = "Verified developers the user doesn't follow yet, ranked by how many topics they share with "
                    + "the user, then by follower count, then newest first. Each suggestion carries the reason to show: "
                    + "SHARED_TOPICS with the shared slugs, or POPULAR when nothing is shared. Empty when there's "
                    + "nobody left to suggest.")
    @ApiResponse(responseCode = "200", description = "Suggestions, best first", useReturnTypeSchema = true)
    ApiEnvelope<List<SuggestionResponse>> suggestions(
            @Parameter(hidden = true) JwtAuthenticationToken authentication,
            @Parameter(description = "How many to return, 1 to 50", example = "20") int limit);
}
