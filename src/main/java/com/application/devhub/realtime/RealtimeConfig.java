package com.application.devhub.realtime;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class RealtimeConfig implements WebSocketMessageBrokerConfigurer {

    public static final String ENDPOINT = "/ws";
    private static final String QUEUE_PREFIX = "/queue";
    private static final String USER_PREFIX = "/user";

    private final RealtimeProperties properties;
    private final RealtimeSessionRegistry registry;
    private final StompAuthenticationInterceptor authenticationInterceptor;
    private final StompAuthorizationInterceptor authorizationInterceptor;
    private final TaskScheduler messageBrokerTaskScheduler;

    public RealtimeConfig(RealtimeProperties properties,
                          RealtimeSessionRegistry registry,
                          StompAuthenticationInterceptor authenticationInterceptor,
                          StompAuthorizationInterceptor authorizationInterceptor,
                          @Lazy @Qualifier("messageBrokerTaskScheduler") TaskScheduler messageBrokerTaskScheduler) {
        this.properties = properties;
        this.registry = registry;
        this.authenticationInterceptor = authenticationInterceptor;
        this.authorizationInterceptor = authorizationInterceptor;
        this.messageBrokerTaskScheduler = messageBrokerTaskScheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry endpoints) {
        endpoints.addEndpoint(ENDPOINT).setAllowedOriginPatterns(properties.allowedOrigins().toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry broker) {
        long heartbeat = properties.heartbeat().toMillis();
        broker.enableSimpleBroker(QUEUE_PREFIX)
                .setHeartbeatValue(new long[]{heartbeat, heartbeat})
                .setTaskScheduler(messageBrokerTaskScheduler);
        broker.setUserDestinationPrefix(USER_PREFIX);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration transport) {
        transport.setTimeToFirstMessage(Math.toIntExact(properties.timeToFirstMessage().toMillis()));
        transport.addDecoratorFactory(handler -> new RealtimeSessionTracking(handler, registry));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration channel) {
        channel.interceptors(authenticationInterceptor, authorizationInterceptor);
    }
}
