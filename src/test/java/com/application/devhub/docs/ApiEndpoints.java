package com.application.devhub.docs;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Map;

public final class ApiEndpoints {

    private static final String APPLICATION_PACKAGE = "com.application.devhub";
    private static final String TEST_CLASSES_DIRECTORY = "/test-classes/";

    private ApiEndpoints() {
    }

    public static List<Endpoint> of(RequestMappingHandlerMapping handlerMapping) {
        return handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> isApplicationCode(entry.getValue()))
                .flatMap(entry -> endpointsOf(entry).stream())
                .sorted()
                .toList();
    }

    private static boolean isApplicationCode(HandlerMethod handlerMethod) {
        Class<?> type = handlerMethod.getBeanType();
        return type.getPackageName().startsWith(APPLICATION_PACKAGE)
                && !type.getProtectionDomain().getCodeSource().getLocation().getPath().contains(TEST_CLASSES_DIRECTORY);
    }

    private static List<Endpoint> endpointsOf(Map.Entry<RequestMappingInfo, HandlerMethod> entry) {
        RequestMappingInfo info = entry.getKey();
        return info.getMethodsCondition().getMethods().stream()
                .flatMap(method -> info.getPatternValues().stream().map(path -> new Endpoint(method.name(), path)))
                .toList();
    }

    public record Endpoint(String method, String path) implements Comparable<Endpoint> {

        @Override
        public int compareTo(Endpoint other) {
            return (path + method).compareTo(other.path + other.method);
        }

        @Override
        public String toString() {
            return method + " " + path;
        }
    }
}
