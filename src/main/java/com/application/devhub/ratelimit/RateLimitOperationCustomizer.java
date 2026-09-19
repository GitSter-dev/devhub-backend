package com.application.devhub.ratelimit;

import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.common.openapi.ErrorResponseDocumenter;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.IntegerSchema;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RateLimitOperationCustomizer implements OperationCustomizer {

    private final ErrorResponseDocumenter documenter;

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        RateLimited rateLimited = handlerMethod.getMethodAnnotation(RateLimited.class);
        if (rateLimited == null) {
            return operation;
        }
        ErrorCode code = ErrorCode.TOO_MANY_REQUESTS;
        documenter.document(operation, List.of(code));
        operation.getResponses().get(String.valueOf(code.status().value()))
                .addHeaderObject(HttpHeaders.RETRY_AFTER, new Header()
                        .description("Seconds to wait before retrying")
                        .schema(new IntegerSchema()));
        return operation;
    }
}
