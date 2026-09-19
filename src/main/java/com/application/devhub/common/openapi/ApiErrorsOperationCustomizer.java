package com.application.devhub.common.openapi;

import com.application.devhub.common.api.ErrorCode;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.models.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApiErrorsOperationCustomizer implements OperationCustomizer {

    private final ErrorResponseDocumenter documenter;

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        List<ErrorCode> codes = new ArrayList<>();
        if (Arrays.stream(handlerMethod.getMethodParameters()).anyMatch(this::isValidated)) {
            codes.add(ErrorCode.VALIDATION_FAILED);
        }
        if (!isPublic(handlerMethod)) {
            codes.add(ErrorCode.UNAUTHORIZED);
        }
        ApiErrors declared = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), ApiErrors.class);
        if (declared != null) {
            codes.addAll(List.of(declared.value()));
        }
        documenter.document(operation, codes);
        return operation;
    }

    private boolean isValidated(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Valid.class);
    }

    private boolean isPublic(HandlerMethod handlerMethod) {
        SecurityRequirements requirements = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getBeanType(), SecurityRequirements.class);
        return requirements != null && requirements.value().length == 0;
    }
}
