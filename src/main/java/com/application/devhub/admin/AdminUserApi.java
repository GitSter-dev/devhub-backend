package com.application.devhub.admin;

import com.application.devhub.admin.AdminUserViews.UserDetail;
import com.application.devhub.admin.AdminUserViews.UserSummary;
import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.openapi.ApiErrors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

import static com.application.devhub.common.api.ErrorCode.FORBIDDEN;
import static com.application.devhub.common.api.ErrorCode.NOT_FOUND;

@Tag(name = "Admin users", description = "Account lookup for moderators. Administrators only.")
public interface AdminUserApi {

    @Operation(summary = "Find accounts",
            description = "Matches an exact email, or a username or display name with the same typo tolerance as "
                    + "people search. Unlike people search it includes banned, suspended, deactivated, unverified "
                    + "and deleted accounts. Queries shorter than 2 characters return nothing; at most 20 results.")
    @ApiResponse(responseCode = "200", description = "Matching accounts", useReturnTypeSchema = true)
    @ApiErrors({FORBIDDEN})
    ApiEnvelope<List<UserSummary>> search(@Parameter(description = "Email, username or display name") String q);

    @Operation(summary = "Get an account",
            description = "The account's status timestamps, how much they post and report, how often they are "
                    + "reported, and the moderation cases about their content. Their action history is in the "
                    + "audit log, filtered by userId.")
    @ApiResponse(responseCode = "200", description = "The account", useReturnTypeSchema = true)
    @ApiErrors({FORBIDDEN, NOT_FOUND})
    ApiEnvelope<UserDetail> detail(@Parameter(description = "The account") UUID userId);
}
