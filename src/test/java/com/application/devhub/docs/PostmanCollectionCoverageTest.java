package com.application.devhub.docs;

import com.application.devhub.IntegrationTest;
import com.application.devhub.docs.ApiEndpoints.Endpoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.server.PathContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
        List<Endpoint> requests = collectRequests(jsonMapper.readTree(COLLECTION.toFile()));

        assertThat(ApiEndpoints.of(handlerMapping)).allSatisfy(endpoint ->
                assertThat(requests.stream().filter(request -> matches(endpoint, request)).count())
                        .as("Postman requests for %s", endpoint)
                        .isGreaterThanOrEqualTo(MINIMUM_REQUESTS_PER_ENDPOINT));
    }

    private static boolean matches(Endpoint endpoint, Endpoint request) {
        PathPattern pattern = PathPatternParser.defaultInstance.parse(endpoint.path());
        return endpoint.method().equals(request.method()) && pattern.matches(PathContainer.parsePath(request.path()));
    }

    private List<Endpoint> collectRequests(JsonNode node) {
        List<Endpoint> endpoints = new ArrayList<>();
        JsonNode request = node.path("request");
        if (!request.isMissingNode()) {
            String path = request.path("url").path("raw").asString().replace(BASE_URL, "").split("\\?", 2)[0];
            endpoints.add(new Endpoint(request.path("method").asString(), path));
        }
        node.path("item").forEach(child -> endpoints.addAll(collectRequests(child)));
        return endpoints;
    }
}
