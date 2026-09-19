package com.application.devhub.realtime;

import com.application.devhub.security.JwtConfig;
import com.application.devhub.session.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StompAuthenticationInterceptorTest {

    private static final UUID USER = UUID.randomUUID();
    private static final UUID SESSION = UUID.randomUUID();

    private final JwtDecoder decoder = mock(JwtDecoder.class);
    private final RefreshTokenService sessions = mock(RefreshTokenService.class);
    private final RealtimeSessionRegistry registry = mock(RealtimeSessionRegistry.class);
    private final StompAuthenticationInterceptor interceptor =
            new StompAuthenticationInterceptor(decoder, new JwtAuthenticationConverter(), sessions, registry);

    @Test
    void aValidTokenOfAnActiveSessionAuthenticatesTheSocket() {
        when(decoder.decode("good")).thenReturn(jwt(SESSION.toString()));
        when(registry.identify(StompFrames.SOCKET_ID, USER, SESSION)).thenReturn(true);
        when(sessions.isSessionActive(SESSION)).thenReturn(true);

        Message<?> accepted = interceptor.preSend(StompFrames.connect("Bearer good"), null);

        assertThat(StompFrames.headersOf(accepted).getUser().getName()).isEqualTo(USER.toString());
    }

    @Test
    void aTokenWithoutTheSessionClaimIsUnauthorized() {
        when(decoder.decode("no-sid")).thenReturn(jwt(null));

        assertRejectedWith("Bearer no-sid", "UNAUTHORIZED");
        verify(registry, never()).identify(any(), any(), any());
    }

    @Test
    void anUndecodableTokenIsUnauthorized() {
        when(decoder.decode("bad")).thenThrow(new BadJwtException("bad signature"));

        assertRejectedWith("Bearer bad", "UNAUTHORIZED");
    }

    @Test
    void aSchemeOtherThanBearerIsUnauthorized() {
        assertRejectedWith("Basic abc", "UNAUTHORIZED");
    }

    @Test
    void aSocketThatClosedWhileAuthenticatingIsRejectedAsEnded() {
        when(decoder.decode("good")).thenReturn(jwt(SESSION.toString()));
        when(registry.identify(StompFrames.SOCKET_ID, USER, SESSION)).thenReturn(false);

        assertRejectedWith("Bearer good", "SESSION_ENDED");
    }

    @Test
    void framesOtherThanConnectPassThroughUntouched() {
        Message<byte[]> subscribe = StompFrames.frame(StompCommand.SUBSCRIBE, "/user/queue/x", null);

        assertThat(interceptor.preSend(subscribe, null)).isSameAs(subscribe);
        verify(decoder, never()).decode(any());
    }

    @Test
    void heartbeatsPassThroughUntouched() {
        Message<byte[]> heartbeat = StompFrames.heartbeat();

        assertThat(interceptor.preSend(heartbeat, null)).isSameAs(heartbeat);
    }

    private void assertRejectedWith(String authorization, String code) {
        assertThatThrownBy(() -> interceptor.preSend(StompFrames.connect(authorization), null))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessage(code);
    }

    private static Jwt jwt(String sessionClaim) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256").subject(USER.toString());
        if (sessionClaim != null) {
            builder.claim(JwtConfig.SESSION_CLAIM, sessionClaim);
        }
        return builder.build();
    }
}
