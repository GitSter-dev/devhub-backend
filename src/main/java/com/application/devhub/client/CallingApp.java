package com.application.devhub.client;

import jakarta.servlet.http.HttpServletRequest;

public final class CallingApp {

    private static final ClientVersion BEFORE_VERSION_HEADERS = new ClientVersion(1, 0, 0);

    private CallingApp() {
    }

    public static ClientVersion versionOf(HttpServletRequest request) {
        if (request.getHeader(ClientVersionFilter.PLATFORM_HEADER) == null) {
            return BEFORE_VERSION_HEADERS;
        }
        return ClientVersion.parse(request.getHeader(ClientVersionFilter.VERSION_HEADER)).orElse(BEFORE_VERSION_HEADERS);
    }
}
