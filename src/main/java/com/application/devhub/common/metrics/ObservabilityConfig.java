package com.application.devhub.common.metrics;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@Configuration(proxyBeanMethods = false)
public class ObservabilityConfig {

    /**
     * The container health check polls /actuator every few seconds. Recording those as
     * requests would bury real traffic in the charts and fill the trace store with noise.
     */
    @Bean
    ObservationPredicate ignoreActuatorRequests() {
        return (name, context) -> !(context instanceof ServerRequestObservationContext request
                && request.getCarrier().getRequestURI().startsWith("/actuator"));
    }
}
