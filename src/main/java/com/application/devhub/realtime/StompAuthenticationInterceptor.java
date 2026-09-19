package com.application.devhub.realtime;

import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.security.JwtConfig;
import com.application.devhub.session.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthenticationInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<StompCommand> CONNECT_COMMANDS = EnumSet.of(StompCommand.CONNECT, StompCommand.STOMP);

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final RefreshTokenService refreshTokenService;
    private final RealtimeSessionRegistry registry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !CONNECT_COMMANDS.contains(accessor.getCommand())) {
            return message;
        }
        Jwt jwt = decode(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION));
        String sessionClaim = jwt.getClaimAsString(JwtConfig.SESSION_CLAIM);
        if (sessionClaim == null) {
            throw StompRejection.of(ErrorCode.UNAUTHORIZED);
        }
        JwtAuthenticationToken authentication = (JwtAuthenticationToken) jwtAuthenticationConverter.convert(jwt);
        UUID userId = UUID.fromString(authentication.getName());
        UUID sessionId = UUID.fromString(sessionClaim);
        if (!registry.identify(accessor.getSessionId(), userId, sessionId)
                || !refreshTokenService.isSessionActive(sessionId)) {
            throw StompRejection.of(ErrorCode.SESSION_ENDED);
        }
        accessor.setUser(authentication);
        log.info("Realtime connected user={} session={}", userId, sessionId);
        return message;
    }

    private Jwt decode(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw StompRejection.of(ErrorCode.UNAUTHORIZED);
        }
        try {
            return jwtDecoder.decode(authorization.substring(BEARER_PREFIX.length()));
        } catch (JwtException e) {
            throw StompRejection.of(ErrorCode.UNAUTHORIZED);
        }
    }
}
