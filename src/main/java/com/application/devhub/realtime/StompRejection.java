package com.application.devhub.realtime;

import com.application.devhub.common.api.ErrorCode;
import org.springframework.messaging.MessageDeliveryException;

final class StompRejection {

    private StompRejection() {
    }

    static MessageDeliveryException of(ErrorCode code) {
        return new MessageDeliveryException(code.name());
    }
}
