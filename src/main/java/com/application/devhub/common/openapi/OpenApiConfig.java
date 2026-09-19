package com.application.devhub.common.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";
    public static final String ERROR_ENVELOPE_SCHEMA = "ErrorEnvelope";

    @Bean
    OpenAPI devHubOpenApi(ApiDocsProperties properties) {
        return new OpenAPI()
                .info(new Info()
                        .title(properties.title())
                        .version(properties.version())
                        .description(properties.description()))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSchemas(ERROR_ENVELOPE_SCHEMA, errorEnvelopeSchema()))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    private static Schema<?> errorEnvelopeSchema() {
        Schema<?> error = new ObjectSchema()
                .addProperty("code", new StringSchema().description("Stable error code the client switches on"))
                .addProperty("message", new StringSchema().description("Localized, human-readable message"))
                .addProperty("fieldErrors", new MapSchema()
                        .additionalProperties(new StringSchema())
                        .description("Present only for VALIDATION_FAILED: field name to message"));
        error.setRequired(List.of("code", "message"));
        Schema<?> envelope = new ObjectSchema()
                .addProperty("success", new BooleanSchema().example(false))
                .addProperty("error", error)
                .addProperty("timestamp", new DateTimeSchema());
        envelope.setRequired(List.of("success", "error", "timestamp"));
        return envelope;
    }
}
