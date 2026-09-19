package com.application.devhub.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiEnvelope<T>(boolean success, T data, ApiError error, Instant timestamp) {

    public static <T> ApiEnvelope<T> ok(T data) {
        return new ApiEnvelope<>(true, data, null, Instant.now());
    }

    public static ApiEnvelope<Void> ok() {
        return ok(null);
    }

    public static ApiEnvelope<Void> error(ErrorCode code, String message) {
        return error(code, message, null);
    }

    public static ApiEnvelope<Void> error(ErrorCode code, String message, Map<String, String> fieldErrors) {
        return new ApiEnvelope<>(false, null, new ApiError(code.name(), message, fieldErrors), Instant.now());
    }
}
