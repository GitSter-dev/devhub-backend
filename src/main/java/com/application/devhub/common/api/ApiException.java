package com.application.devhub.common.api;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] messageArgs;

    protected ApiException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }

    public static ApiException of(ErrorCode errorCode, Object... messageArgs) {
        return new ApiException(errorCode, messageArgs);
    }

    public static ApiException badRequest() {
        return of(ErrorCode.BAD_REQUEST);
    }

    public static ApiException unauthorized() {
        return of(ErrorCode.UNAUTHORIZED);
    }

    public static ApiException forbidden() {
        return of(ErrorCode.FORBIDDEN);
    }

    public static ApiException notFound() {
        return of(ErrorCode.NOT_FOUND);
    }

    public static ApiException conflict() {
        return of(ErrorCode.CONFLICT);
    }

    public static ApiException tooManyRequests() {
        return of(ErrorCode.TOO_MANY_REQUESTS);
    }
}
