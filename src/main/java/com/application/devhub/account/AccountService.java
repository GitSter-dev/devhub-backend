package com.application.devhub.account;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.session.RevocationReason;
import com.application.devhub.session.RefreshTokenService;
import com.application.devhub.session.SessionService;
import com.application.devhub.session.TokenPair;
import com.application.devhub.user.User;
import com.application.devhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final SessionService sessionService;

    @Transactional
    public void requestDeletion(UUID userId, String password) {
        User user = userRepository.findById(userId).orElseThrow(ApiException::notFound);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.of(ErrorCode.INVALID_CREDENTIALS);
        }
        user.deactivate();
        refreshTokenService.revokeAllFor(userId, RevocationReason.ACCOUNT_DELETED);
    }

    @Transactional
    public TokenPair restore(String identifier, String password) {
        User user = userRepository.findByLogin(identifier).orElseThrow(ApiException::notFound);
        if (user.isDeleted() || !user.isDeactivated()
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.of(ErrorCode.INVALID_CREDENTIALS);
        }
        user.reactivate();
        userRepository.saveAndFlush(user);
        return sessionService.login(identifier, password);
    }
}
