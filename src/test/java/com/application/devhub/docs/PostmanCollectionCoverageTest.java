package com.application.devhub.docs;

import com.application.devhub.IntegrationTest;
import com.application.devhub.docs.ApiEndpoints.Endpoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PostmanCollectionCoverageTest extends IntegrationTest {

    private static final Path COLLECTION = Path.of("postman/DevHub.postman_collection.json");
    private static final String BASE_URL = "{{baseUrl}}";
    private static final int MINIMUM_REQUESTS_PER_ENDPOINT = 2;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void everyEndpointHasAHappyPathAndEdgeCasesInPostman() {
        Map<Endpoint, Long> requestsPerEndpoint = collectRequests(jsonMapper.readTree(COLLECTION.toFile()))
                .stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        assertThat(ApiEndpoints.of(handlerMapping)).allSatisfy(endpoint ->
                assertThat(requestsPerEndpoint.getOrDefault(endpoint, 0L))
                        .as("Postman requests for %s", endpoint)
                        .isGreaterThanOrEqualTo(MINIMUM_REQUESTS_PER_ENDPOINT));
    }

    private List<Endpoint> collectRequests(JsonNode node) {
        List<Endpoint> endpoints = new ArrayList<>();
        JsonNode request = node.path("request");
        if (!request.isMissingNode()) {
            String raw = request.path("url").path("raw").asString();
            endpoints.add(new Endpoint(request.path("method").asString(), raw.replace(BASE_URL, "")));
        }
        node.path("item").forEach(child -> endpoints.addAll(collectRequests(child)));
        return endpoints;
    }
}
