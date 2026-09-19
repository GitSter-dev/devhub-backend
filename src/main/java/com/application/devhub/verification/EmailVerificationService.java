package com.application.devhub.verification;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.otp.OneTimeCodeIssuer;
import com.application.devhub.otp.OneTimeCodePurpose;
import com.application.devhub.otp.OneTimeCodeVerifier;
import com.application.devhub.outbox.OutboxEventType;
import com.application.devhub.outbox.OutboxPublisher;
import com.application.devhub.user.User;
import com.application.devhub.user.UserEventPayload;
import com.application.devhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final OneTimeCodePurpose PURPOSE = OneTimeCodePurpose.EMAIL_VERIFICATION;

    private final UserRepository userRepository;
    private final OneTimeCodeIssuer codeIssuer;
    private final OneTimeCodeVerifier codeVerifier;
    private final OutboxPublisher outboxPublisher;

    @Transactional
    public void requestVerification(User user) {
        outboxPublisher.publish(OutboxEventType.EMAIL_VERIFICATION, new UserEventPayload(user.getId()));
    }

    @Transactional
    public void resend(String email) {
        userRepository.findByEmail(email)
                .filter(user -> !user.isEmailVerified())
                .filter(user -> codeIssuer.canIssue(user, PURPOSE))
                .ifPresent(this::requestVerification);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void verify(String email, String code) {
        Optional<User> user = userRepository.findByEmail(email).filter(candidate -> !candidate.isEmailVerified());
        if (!codeVerifier.verify(user, PURPOSE, code)) {
            throw ApiException.of(ErrorCode.INVALID_VERIFICATION_CODE);
        }
        user.get().markEmailVerified();
    }
}
