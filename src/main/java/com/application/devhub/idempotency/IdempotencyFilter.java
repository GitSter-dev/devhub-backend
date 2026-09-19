package com.application.devhub.idempotency;

import com.application.devhub.common.api.EnvelopeWriter;
import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.common.crypto.Sha256;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Pattern VALID_KEY = Pattern.compile("^[A-Za-z0-9_-]{8,128}$");
    private static final String ANONYMOUS_SCOPE = "anonymous";

    private final RequestMappingHandlerMapping handlerMapping;
    private final IdempotencyRecordStore store;
    private final EnvelopeWriter envelopeWriter;

    public IdempotencyFilter(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
                             IdempotencyRecordStore store, EnvelopeWriter envelopeWriter) {
        this.handlerMapping = handlerMapping;
        this.store = store;
        this.envelopeWriter = envelopeWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || request.getHeader(Idempotent.HEADER) == null
                || !isIdempotentEndpoint(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String clientKey = request.getHeader(Idempotent.HEADER);
        if (!VALID_KEY.matcher(clientKey).matches()) {
            envelopeWriter.writeError(response, ErrorCode.BAD_REQUEST);
            return;
        }

        CachedBodyRequest cachedRequest = new CachedBodyRequest(request);
        String key = scope() + ":" + clientKey;
        String endpoint = request.getMethod() + " " + request.getRequestURI();
        String requestHash = Sha256.hex(cachedRequest.body());

        Claim claim = store.claim(key, endpoint, requestHash);
        if (!claim.isGranted()) {
            replayOrReject(claim.existing().orElseThrow(), requestHash, response);
            return;
        }

        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(cachedRequest, cachedResponse);
        } catch (IOException | ServletException | RuntimeException e) {
            store.release(key, endpoint);
            throw e;
        }

        int status = cachedResponse.getStatus();
        if (ReplayPolicy.isReplayable(status) && isJson(cachedResponse)) {
            store.complete(key, endpoint, status, bodyOf(cachedResponse));
        } else {
            store.release(key, endpoint);
        }
        cachedResponse.copyBodyToResponse();
    }

    private void replayOrReject(IdempotencyRecord record, String requestHash, HttpServletResponse response)
            throws IOException {
        if (!record.matches(requestHash)) {
            envelopeWriter.writeError(response, ErrorCode.IDEMPOTENCY_KEY_REUSED);
            return;
        }
        if (!record.isCompleted()) {
            envelopeWriter.writeError(response, ErrorCode.IDEMPOTENCY_IN_PROGRESS);
            return;
        }
        response.setStatus(record.getResponseStatus());
        response.setHeader(Idempotent.REPLAYED_HEADER, "true");
        if (record.getResponseBody() != null) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getOutputStream().write(record.getResponseBody().getBytes(StandardCharsets.UTF_8));
        }
    }

    private boolean isIdempotentEndpoint(HttpServletRequest request) {
        try {
            HandlerExecutionChain handler = handlerMapping.getHandler(request);
            return handler != null
                    && handler.getHandler() instanceof HandlerMethod method
                    && method.hasMethodAnnotation(Idempotent.class);
        } catch (Exception e) {
            return false;
        }
    }

    private String scope() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof JwtAuthenticationToken jwt ? jwt.getName() : ANONYMOUS_SCOPE;
    }

    private boolean isJson(ContentCachingResponseWrapper response) {
        String contentType = response.getContentType();
        return contentType == null || MediaType.parseMediaType(contentType).isCompatibleWith(MediaType.APPLICATION_JSON);
    }

    private String bodyOf(ContentCachingResponseWrapper response) {
        byte[] body = response.getContentAsByteArray();
        return body.length == 0 ? null : new String(body, StandardCharsets.UTF_8);
    }
}
