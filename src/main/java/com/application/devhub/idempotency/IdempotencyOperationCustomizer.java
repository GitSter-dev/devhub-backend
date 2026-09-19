package com.application.devhub.idempotency;

import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.common.openapi.ErrorResponseDocumenter;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.List;

@Component
@RequiredArgsConstructor
public class IdempotencyOperationCustomizer implements OperationCustomizer {

    private final ErrorResponseDocumenter documenter;

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        if (!handlerMethod.hasMethodAnnotation(Idempotent.class)) {
            return operation;
        }
        operation.addParametersItem(new HeaderParameter()
                .name(Idempotent.HEADER)
                .required(false)
                .description("Client-generated key (8-128 chars of A-Z, a-z, 0-9, - or _) for one logical operation. "
                        + "Retries with the same key and body replay the original response with an "
                        + Idempotent.REPLAYED_HEADER + " header instead of running twice.")
                .schema(new StringSchema().example("3f2b8c1e-4a7d-4f0e-9c1a-2b3c4d5e6f70")));
        documenter.document(operation, List.of(ErrorCode.IDEMPOTENCY_IN_PROGRESS, ErrorCode.IDEMPOTENCY_KEY_REUSED));
        return operation;
    }
}
