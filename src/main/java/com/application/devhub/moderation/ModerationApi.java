package com.application.devhub.moderation;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.openapi.ApiErrors;
import com.application.devhub.moderation.ModerationViews.CaseDetail;
import com.application.devhub.moderation.ModerationViews.CasePage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

import static com.application.devhub.common.api.ErrorCode.BAD_REQUEST;
import static com.application.devhub.common.api.ErrorCode.FORBIDDEN;
import static com.application.devhub.common.api.ErrorCode.NOT_FOUND;
import static com.application.devhub.common.api.ErrorCode.VALIDATION_FAILED;

@Tag(name = "Moderation", description = "The moderation queue. Administrators only.")
public interface ModerationApi {

    @Operation(summary = "List moderation cases",
            description = "One case per reported item, most serious first: highest reason weight, then the number of "
                    + "distinct reporters, then most recently reported.")
    @ApiResponse(responseCode = "200", description = "A page of cases", useReturnTypeSchema = true)
    @ApiErrors({FORBIDDEN, BAD_REQUEST})
    ApiEnvelope<CasePage> cases(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                @Parameter(description = "Which cases to list") CaseStatus status,
                                @Parameter(description = "Only cases about content in this community") UUID communityId,
                                @Parameter(description = "nextCursor from the previous page") String cursor);

    @Operation(summary = "Get a moderation case",
            description = "The case with every report, each carrying the snapshot taken when it was filed, and the "
                    + "actions taken so far.")
    @ApiResponse(responseCode = "200", description = "The case", useReturnTypeSchema = true)
    @ApiErrors({FORBIDDEN, NOT_FOUND})
    ApiEnvelope<CaseDetail> caseDetail(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                       @Parameter(description = "The case") UUID caseId);

    @Operation(summary = "Act on a case",
            description = "DISMISS and RESTORE put hidden or removed content back and close the case. REMOVE_CONTENT "
                    + "takes the post or message down and tells the reporters. SUSPEND needs days and signs the "
                    + "person out everywhere until it expires; BAN does the same permanently and hides their "
                    + "content. REINSTATE lifts a suspension or ban. Every action is written to the audit log.")
    @ApiResponse(responseCode = "200", description = "The updated case", useReturnTypeSchema = true)
    @ApiErrors({FORBIDDEN, NOT_FOUND, BAD_REQUEST, VALIDATION_FAILED})
    ApiEnvelope<CaseDetail> act(@Parameter(hidden = true) JwtAuthenticationToken authentication,
                                @Parameter(description = "The case") UUID caseId,
                                ModerationActionRequest request);
}
