package com.application.devhub.docs;

import com.application.devhub.IntegrationTest;
import com.application.devhub.docs.ApiEndpoints.Endpoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class ApiDocumentationIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void everyEndpointIsDocumented() throws Exception {
        JsonNode paths = jsonMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
                .andReturn().getResponse().getContentAsString()).get("paths");
        List<Endpoint> endpoints = ApiEndpoints.of(handlerMapping);

        assertThat(endpoints).isNotEmpty();
        assertThat(endpoints).allSatisfy(endpoint -> {
            JsonNode operation = paths.path(endpoint.path()).path(endpoint.method().toLowerCase());
            assertThat(operation.isMissingNode()).as("%s is missing from the OpenAPI document", endpoint).isFalse();
            assertThat(operation.path("summary").asString()).as("%s has no summary", endpoint).isNotBlank();
            assertThat(operation.path("description").asString()).as("%s has no description", endpoint).isNotBlank();
        });
    }
}
