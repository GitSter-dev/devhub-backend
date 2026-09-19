package com.application.devhub.user;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.api.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AccountAccess {

    public void ensureCanSignIn(User user) {
        if (user.isBanned()) {
            throw ApiException.of(ErrorCode.ACCOUNT_BANNED);
        }
        if (user.isSuspendedAt(Instant.now())) {
            throw ApiException.of(ErrorCode.ACCOUNT_SUSPENDED, user.getSuspendedUntil());
        }
        if (user.isDeactivated()) {
            throw ApiException.of(ErrorCode.ACCOUNT_DEACTIVATED);
        }
    }

    public boolean canContinueSession(User user) {
        return user.isEmailVerified() && !user.isBanned() && !user.isDeactivated()
                && !user.isSuspendedAt(Instant.now());
    }
}
