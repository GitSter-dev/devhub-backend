package com.application.devhub.ratelimit;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.api.ErrorCode;
import lombok.Getter;

import java.time.Duration;

@Getter
public class RateLimitExceededException extends ApiException {

    private final Duration retryAfter;

    public RateLimitExceededException(Duration retryAfter) {
        super(ErrorCode.TOO_MANY_REQUESTS);
        this.retryAfter = retryAfter;
    }
}
