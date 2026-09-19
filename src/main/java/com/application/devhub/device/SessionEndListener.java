package com.application.devhub.device;

import com.application.devhub.session.SessionEnded;
import com.application.devhub.session.UserSessionsEnded;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionEndListener {

    private final DeviceRegistry registry;

    @EventListener
    public void onUserSessionsEnded(UserSessionsEnded event) {
        registry.revokeAllForUser(event.userId());
    }

    @EventListener
    public void onSessionEnded(SessionEnded event) {
        registry.revokeAllForSession(event.familyId());
    }
}
