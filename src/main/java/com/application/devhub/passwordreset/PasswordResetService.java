package com.application.devhub.passwordreset;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.otp.OneTimeCodeIssuer;
import com.application.devhub.otp.OneTimeCodePurpose;
import com.application.devhub.otp.OneTimeCodeVerifier;
import com.application.devhub.outbox.OutboxEventType;
import com.application.devhub.outbox.OutboxPublisher;
import com.application.devhub.session.RefreshTokenService;
import com.application.devhub.session.RevocationReason;
import com.application.devhub.user.User;
import com.application.devhub.user.UserEventPayload;
import com.application.devhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final OneTimeCodePurpose PURPOSE = OneTimeCodePurpose.PASSWORD_RESET;

    private final UserRepository userRepository;
    private final OneTimeCodeIssuer codeIssuer;
    private final OneTimeCodeVerifier codeVerifier;
    private final OutboxPublisher outboxPublisher;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(email)
                .filter(User::isEmailVerified)
                .filter(user -> codeIssuer.canIssue(user, PURPOSE))
                .ifPresent(user -> outboxPublisher.publish(OutboxEventType.PASSWORD_RESET,
                        new UserEventPayload(user.getId())));
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void reset(String email, String code, String newPassword) {
        Optional<User> user = userRepository.findByEmail(email).filter(User::isEmailVerified);
        if (!codeVerifier.verify(user, PURPOSE, code)) {
            throw ApiException.of(ErrorCode.INVALID_RESET_CODE);
        }
        User account = user.get();
        account.changePassword(passwordEncoder.encode(newPassword));
        refreshTokenService.revokeAllFor(account.getId(), RevocationReason.PASSWORD_RESET);
        outboxPublisher.publish(OutboxEventType.PASSWORD_CHANGED, new UserEventPayload(account.getId()));
    }
}
