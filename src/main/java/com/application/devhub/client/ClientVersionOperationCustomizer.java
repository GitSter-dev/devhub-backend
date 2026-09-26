package com.application.devhub.client;

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
public class ClientVersionOperationCustomizer implements OperationCustomizer {

    private final ErrorResponseDocumenter documenter;

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        operation.addParametersItem(new HeaderParameter()
                .name(ClientVersionFilter.PLATFORM_HEADER)
                .required(false)
                .description("Platform of the calling app. Together with " + ClientVersionFilter.VERSION_HEADER
                        + ", lets the server turn away builds that are too old to talk to this API.")
                .schema(new StringSchema()._enum(List.of("android", "ios")).example("android")));
        operation.addParametersItem(new HeaderParameter()
                .name(ClientVersionFilter.VERSION_HEADER)
                .required(false)
                .description("Version of the calling app, as MAJOR.MINOR.PATCH.")
                .schema(new StringSchema().example("1.0.1")));
        documenter.document(operation, List.of(ErrorCode.APP_UPDATE_REQUIRED));
        return operation;
    }
}
