package com.application.devhub.realtime;

import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.session.RevocationReason;
import org.springframework.web.socket.CloseStatus;

public final class RealtimeCloseStatus {

    public static final CloseStatus SESSION_REPLACED = new CloseStatus(4001, ErrorCode.SESSION_REPLACED.name());
    public static final CloseStatus SESSION_ENDED = new CloseStatus(4002, ErrorCode.SESSION_ENDED.name());

    private RealtimeCloseStatus() {
    }

    public static CloseStatus of(RevocationReason reason) {
        return reason == RevocationReason.REPLACED ? SESSION_REPLACED : SESSION_ENDED;
    }
}
