package com.application.devhub.admin;

import com.application.devhub.admin.AdminStatsViews.Overview;
import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.openapi.ApiErrors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.application.devhub.common.api.ErrorCode.FORBIDDEN;

@Tag(name = "Admin stats", description = "Platform health at a glance. Administrators only.")
public interface AdminStatsApi {

    @Operation(summary = "Get the stats overview",
            description = "Current totals (live accounts, verified accounts, live posts, open cases, suspended and "
                    + "banned accounts) and one row per UTC day with signups, posts, reports and moderation actions. "
                    + "days is clamped to 1..90.")
    @ApiResponse(responseCode = "200", description = "The overview", useReturnTypeSchema = true)
    @ApiErrors({FORBIDDEN})
    ApiEnvelope<Overview> overview(@Parameter(description = "How many days of history, today included") int days);
}
