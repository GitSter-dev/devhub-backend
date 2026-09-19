package com.application.devhub.block;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.openapi.ApiErrors;
import com.application.devhub.profile.PersonPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

import static com.application.devhub.common.api.ErrorCode.BAD_REQUEST;
import static com.application.devhub.common.api.ErrorCode.CANNOT_BLOCK_SELF;
import static com.application.devhub.common.api.ErrorCode.NOT_FOUND;

@Tag(name = "Blocks", description = "Blocking developers you don't want to hear from")
public interface BlockApi {

    @Operation(summary = "Block a developer",
            description = "From now on neither of you sees the other's posts, replies, profile or search results, "
                    + "follows between you are removed both ways, notifications between you are withdrawn and "
                    + "your direct conversation disappears for both. Groups you share are unaffected. Blocking "
                    + "someone already blocked succeeds and changes nothing.")
    @ApiResponse(responseCode = "200", description = "Blocked", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, CANNOT_BLOCK_SELF})
    ApiEnvelope<Void> block(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                            @Parameter(description = "The user to block") UUID userId);

    @Operation(summary = "Unblock a developer",
            description = "Lifts the block. Follows that the block removed are not restored.")
    @ApiResponse(responseCode = "200", description = "Unblocked", useReturnTypeSchema = true)
    ApiEnvelope<Void> unblock(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                              @Parameter(description = "The user to unblock") UUID userId);

    @Operation(summary = "List blocked developers", description = "Most recently blocked first, 30 per page.")
    @ApiResponse(responseCode = "200", description = "A page of blocked developers", useReturnTypeSchema = true)
    @ApiErrors(BAD_REQUEST)
    ApiEnvelope<PersonPage> blocked(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                    @Parameter(description = "nextCursor from the previous page") String cursor);
}
