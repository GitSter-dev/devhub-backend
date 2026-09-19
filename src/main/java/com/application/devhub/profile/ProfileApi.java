package com.application.devhub.profile;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.openapi.ApiErrors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static com.application.devhub.common.api.ErrorCode.BAD_REQUEST;
import static com.application.devhub.common.api.ErrorCode.NOT_FOUND;

@Tag(name = "Profiles", description = "Public developer profiles, their followers and people search")
public interface ProfileApi {

    @Operation(summary = "Get a developer's profile",
            description = "Every profile is public. The username is case-insensitive, and a username someone recently "
                    + "gave up still resolves to them while it's held. `me`, `following` and `followsYou` are relative "
                    + "to the signed-in user. NOT_FOUND for unknown or unverified accounts.")
    @ApiResponse(responseCode = "200", description = "The profile", useReturnTypeSchema = true)
    @ApiErrors(NOT_FOUND)
    ApiEnvelope<ProfileResponse> profile(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                         @Parameter(description = "The developer's username", example = "ada_rust")
                                         String username);

    @Operation(summary = "Edit the signed-in user's profile",
            description = "Replaces the display name, bio, GitHub username and website. Optional fields that are "
                    + "missing, null or blank are cleared. The username is changed separately.")
    @ApiResponse(responseCode = "200", description = "The updated profile", useReturnTypeSchema = true)
    ApiEnvelope<ProfileResponse> updateProfile(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                               UpdateProfileRequest request);

    @Operation(summary = "List a developer's followers",
            description = "Newest first, 20 per page. Pass the previous page's nextCursor to continue; nextCursor is "
                    + "null on the last page. `following` says whether the signed-in user follows each person.")
    @ApiResponse(responseCode = "200", description = "A page of followers", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, BAD_REQUEST})
    ApiEnvelope<PersonPage> followers(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                      @Parameter(description = "The developer's username") String username,
                                      @Parameter(description = "nextCursor from the previous page") String cursor);

    @Operation(summary = "List who a developer follows",
            description = "Newest first, 20 per page, paginated like the followers list.")
    @ApiResponse(responseCode = "200", description = "A page of followed developers", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, BAD_REQUEST})
    ApiEnvelope<PersonPage> following(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                      @Parameter(description = "The developer's username") String username,
                                      @Parameter(description = "nextCursor from the previous page") String cursor);

    @Operation(summary = "Search developers by name or username",
            description = "Matches username prefixes, the start of any word in the display name (accents ignored) "
                    + "and close misspellings. Exact and prefix matches come first, then the closest fuzzy matches, "
                    + "then people the user follows and popular developers. A leading @ is ignored. Never includes "
                    + "the signed-in user.")
    @ApiResponse(responseCode = "200", description = "Matching developers, best first", useReturnTypeSchema = true)
    ApiEnvelope<List<PersonSummary>> search(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                            @Parameter(description = "Name or username to look for", example = "ada")
                                            String q,
                                            @Parameter(description = "How many to return, 1 to 50", example = "20")
                                            int limit);
}
