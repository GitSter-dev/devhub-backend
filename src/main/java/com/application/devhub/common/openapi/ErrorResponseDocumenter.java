package com.application.devhub.common.openapi;

import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.common.api.ErrorMessageResolver;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ErrorResponseDocumenter {

    private static final String JSON = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
    private static final String EXAMPLE_TIMESTAMP = "2026-01-01T12:00:00Z";

    private final ErrorMessageResolver messageResolver;

    public void document(Operation operation, Collection<ErrorCode> codes) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        Map<Integer, List<ErrorCode>> byStatus = codes.stream()
                .distinct()
                .collect(Collectors.groupingBy(code -> code.status().value(), LinkedHashMap::new, Collectors.toList()));
        byStatus.forEach((status, statusCodes) -> merge(operation.getResponses(), String.valueOf(status), statusCodes));
    }

    private void merge(ApiResponses responses, String status, List<ErrorCode> codes) {
        ApiResponse response = responses.get(status);
        if (response == null || response.getContent() == null || response.getContent().get(JSON) == null) {
            response = new ApiResponse().content(new Content().addMediaType(JSON,
                    new MediaType().schema(new Schema<>().$ref(OpenApiConfig.ERROR_ENVELOPE_SCHEMA))));
            responses.addApiResponse(status, response);
        }
        MediaType mediaType = response.getContent().get(JSON);
        codes.forEach(code -> mediaType.addExamples(code.name(), new Example()
                .summary(messageResolver.resolve(code))
                .value(exampleBody(code))));
        response.setDescription(String.join(", ", mediaType.getExamples().keySet()));
    }

    private Map<String, Object> exampleBody(ErrorCode code) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code.name());
        error.put("message", messageResolver.resolve(code));
        if (code == ErrorCode.VALIDATION_FAILED) {
            error.put("fieldErrors", Map.of("email", "must be a well-formed email address"));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", error);
        body.put("timestamp", EXAMPLE_TIMESTAMP);
        return body;
    }
}
