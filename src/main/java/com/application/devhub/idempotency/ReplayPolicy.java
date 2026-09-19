package com.application.devhub.idempotency;

import org.springframework.http.HttpStatus;

public final class ReplayPolicy {

    private ReplayPolicy() {
    }

    public static boolean isReplayable(int status) {
        return status < HttpStatus.INTERNAL_SERVER_ERROR.value()
                && status != HttpStatus.REQUEST_TIMEOUT.value()
                && status != HttpStatus.TOO_MANY_REQUESTS.value();
    }
}
