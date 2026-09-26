package com.application.devhub.moderation;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.openapi.ApiErrors;
import com.application.devhub.moderation.ModerationViews.AuditPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

import static com.application.devhub.common.api.ErrorCode.BAD_REQUEST;
import static com.application.devhub.common.api.ErrorCode.FORBIDDEN;

@Tag(name = "Moderation", description = "The moderation queue. Administrators only.")
public interface AuditLogApi {

    @Operation(summary = "Read the moderation audit log",
            description = "Every moderation action, newest first, with who took it, against whom and why. "
                    + "Filter by userId to see one person's history.")
    @ApiResponse(responseCode = "200", description = "A page of audit entries", useReturnTypeSchema = true)
    @ApiErrors({FORBIDDEN, BAD_REQUEST})
    ApiEnvelope<AuditPage> entries(@Parameter(description = "Only actions taken against this user") UUID userId,
                                   @Parameter(description = "nextCursor from the previous page") String cursor);
}
