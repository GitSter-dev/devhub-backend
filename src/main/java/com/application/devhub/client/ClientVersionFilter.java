package com.application.devhub.client;

import com.application.devhub.common.api.EnvelopeWriter;
import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.common.metrics.DevHubMetrics;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
@RequiredArgsConstructor
public class ClientVersionFilter extends OncePerRequestFilter {

    public static final String PLATFORM_HEADER = "X-App-Platform";
    public static final String VERSION_HEADER = "X-App-Version";

    private static final List<String> UNGATED_PREFIXES = List.of("/actuator", "/v3/api-docs", "/swagger-ui", "/ws");

    private final ClientVersionProperties properties;
    private final EnvelopeWriter envelopeWriter;
    private final DevHubMetrics metrics;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return request.getHeader(PLATFORM_HEADER) == null
                || UNGATED_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String platform = request.getHeader(PLATFORM_HEADER);
        Optional<ClientVersion> minimum = properties.minimumFor(platform);
        Optional<ClientVersion> version = ClientVersion.parse(request.getHeader(VERSION_HEADER));

        if (minimum.isPresent() && version.isPresent() && version.get().isOlderThan(minimum.get())) {
            log.info("Rejected outdated client: platform={} version={} minimum={} path={}",
                    platform, version.get(), minimum.get(), request.getRequestURI());
            metrics.outdatedClientRejection(platform, version.get().toString());
            envelopeWriter.writeError(response, ErrorCode.APP_UPDATE_REQUIRED);
            return;
        }
        chain.doFilter(request, response);
    }
}
