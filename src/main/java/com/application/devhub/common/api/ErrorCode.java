package com.application.devhub.common.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.util.Arrays;

public enum ErrorCode {

    BAD_REQUEST(HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
    CONFLICT(HttpStatus.CONFLICT),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_TAKEN(HttpStatus.CONFLICT),
    USERNAME_TAKEN(HttpStatus.CONFLICT),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED),
    INVALID_RESET_CODE(HttpStatus.BAD_REQUEST),
    SESSION_REPLACED(HttpStatus.UNAUTHORIZED),
    IDEMPOTENCY_IN_PROGRESS(HttpStatus.CONFLICT),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.UNPROCESSABLE_CONTENT),
    SESSION_ENDED(HttpStatus.UNAUTHORIZED),
    CANNOT_FOLLOW_SELF(HttpStatus.BAD_REQUEST),
    TOPICS_REQUIRED(HttpStatus.BAD_REQUEST),
    USERNAME_CHANGE_TOO_SOON(HttpStatus.CONFLICT),
    GROUP_TOO_LARGE(HttpStatus.BAD_REQUEST),
    NOT_GROUP_OWNER(HttpStatus.FORBIDDEN),
    CANNOT_MESSAGE_YOURSELF(HttpStatus.BAD_REQUEST),
    CONVERSATION_REQUEST_PENDING(HttpStatus.FORBIDDEN),
    CANNOT_BLOCK_SELF(HttpStatus.BAD_REQUEST);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public static ErrorCode from(HttpStatusCode statusCode) {
        return Arrays.stream(values())
                .filter(code -> code.status.value() == statusCode.value())
                .findFirst()
                .orElse(statusCode.is5xxServerError() ? INTERNAL_ERROR : BAD_REQUEST);
    }
}
