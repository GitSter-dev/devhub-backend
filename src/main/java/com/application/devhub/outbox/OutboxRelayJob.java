package com.application.devhub.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxRelayJob {

    private final OutboxRelay relay;

    @Scheduled(fixedDelayString = "${devhub.outbox.poll-interval}")
    void relayPendingEvents() {
        relay.relayPending();
    }
}
