package com.application.devhub.ratelimit;

public enum RateLimitPolicy {
    SIGNUP,
    LOGIN,
    LOGIN_FAILURES,
    REFRESH,
    VERIFY_EMAIL,
    RESET_PASSWORD,
    RESEND_VERIFICATION,
    FORGOT_PASSWORD,
    TEST_NOTIFICATION,
    FOLLOW,
    SEARCH
}
