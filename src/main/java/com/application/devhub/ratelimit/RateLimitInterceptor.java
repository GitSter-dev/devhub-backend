package com.application.devhub.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod method) {
            RateLimited rateLimited = method.getMethodAnnotation(RateLimited.class);
            if (rateLimited != null) {
                rateLimiter.consume(rateLimited.value(), keyOf(rateLimited.scope(), request));
            }
        }
        return true;
    }

    private static String keyOf(RateLimitScope scope, HttpServletRequest request) {
        Principal user = request.getUserPrincipal();
        return scope == RateLimitScope.USER && user != null ? "user:" + user.getName() : request.getRemoteAddr();
    }
}
