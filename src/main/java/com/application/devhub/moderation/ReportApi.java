package com.application.devhub.moderation;

import com.application.devhub.common.api.ApiEnvelope;
import com.application.devhub.common.openapi.ApiErrors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static com.application.devhub.common.api.ErrorCode.BAD_REQUEST;
import static com.application.devhub.common.api.ErrorCode.NOT_FOUND;
import static com.application.devhub.common.api.ErrorCode.TOO_MANY_REQUESTS;
import static com.application.devhub.common.api.ErrorCode.VALIDATION_FAILED;

@Tag(name = "Reports", description = "Reporting posts, messages and developers to the moderators")
public interface ReportApi {

    @Operation(summary = "Report content or a developer",
            description = "Files a report for the moderators. The reported content is copied as it looks now, so "
                    + "deleting it later doesn't erase the evidence; a reported message also stores the messages "
                    + "around it that you can already see. Reporting the same thing twice changes nothing. "
                    + "NOT_FOUND when the target isn't visible to you, BAD_REQUEST when it's your own.")
    @ApiResponse(responseCode = "200", description = "The report was filed", useReturnTypeSchema = true)
    @ApiErrors({NOT_FOUND, BAD_REQUEST, VALIDATION_FAILED, TOO_MANY_REQUESTS})
    ApiEnvelope<Void> report(@Parameter(hidden = true) JwtAuthenticationToken authentication, ReportRequest request);
}
