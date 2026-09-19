package com.application.devhub.realtime;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StompAuthorizationInterceptorTest {

    private static final Principal USER = () -> "user-1";

    private final StompAuthorizationInterceptor interceptor = new StompAuthorizationInterceptor();

    @Test
    void anAuthenticatedUserMaySubscribeToTheirOwnQueues() {
        assertAllowed(StompFrames.frame(StompCommand.SUBSCRIBE, "/user/queue/notifications", USER));
    }

    @Test
    void unsubscribingAndDisconnectingAreAllowed() {
        assertAllowed(StompFrames.frame(StompCommand.UNSUBSCRIBE, null, USER));
        assertAllowed(StompFrames.frame(StompCommand.DISCONNECT, null, null));
    }

    @Test
    void heartbeatsAreAllowed() {
        assertAllowed(StompFrames.heartbeat());
    }

    @Test
    void subscribingWithoutAnAuthenticatedUserIsUnauthorized() {
        assertThatThrownBy(() -> interceptor.preSend(
                StompFrames.frame(StompCommand.SUBSCRIBE, "/user/queue/notifications", null), null))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessage("UNAUTHORIZED");
    }

    @Test
    void typingIntoAConversationIsTheOnlyAllowedSend() {
        assertAllowed(StompFrames.frame(StompCommand.SEND,
                "/app/conversations/7c9e6679-7425-40de-944b-e07fc1f90ae7/typing", USER));
        assertThatThrownBy(() -> interceptor.preSend(
                StompFrames.frame(StompCommand.SEND, "/app/conversations/not-a-uuid/typing", USER), null))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessage("FORBIDDEN");
        assertThatThrownBy(() -> interceptor.preSend(
                StompFrames.frame(StompCommand.SEND, "/app/anything-else", USER), null))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessage("FORBIDDEN");
    }

    @Test
    void subscribingToAnotherPrefixIsForbidden() {
        assertThatThrownBy(() -> interceptor.preSend(
                StompFrames.frame(StompCommand.SUBSCRIBE, "/queue/everyone", USER), null))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessage("FORBIDDEN");
    }

    private void assertAllowed(Message<byte[]> frame) {
        assertThat(interceptor.preSend(frame, null)).isSameAs(frame);
    }
}
