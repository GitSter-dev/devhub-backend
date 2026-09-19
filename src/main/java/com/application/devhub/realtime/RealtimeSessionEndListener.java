package com.application.devhub.realtime;

import com.application.devhub.session.SessionEnded;
import com.application.devhub.session.UserSessionsEnded;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RealtimeSessionEndListener {

    private final RealtimeSessionRegistry registry;

    @TransactionalEventListener(fallbackExecution = true)
    public void onUserSessionsEnded(UserSessionsEnded event) {
        registry.closeUser(event.userId(), RealtimeCloseStatus.of(event.reason()));
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onSessionEnded(SessionEnded event) {
        registry.closeSession(event.familyId(), RealtimeCloseStatus.of(event.reason()));
    }
}
