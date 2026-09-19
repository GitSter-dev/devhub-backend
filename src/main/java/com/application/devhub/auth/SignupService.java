package com.application.devhub.auth;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.user.User;
import com.application.devhub.user.UserRepository;
import com.application.devhub.user.UsernameService;
import com.application.devhub.verification.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserRepository userRepository;
    private final UsernameService usernameService;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw ApiException.of(ErrorCode.EMAIL_TAKEN);
        }
        if (userRepository.existsByUsername(request.username()) || usernameService.isHeld(request.username())) {
            throw ApiException.of(ErrorCode.USERNAME_TAKEN);
        }
        User user = new User(request.username(), request.displayName(), request.email(),
                passwordEncoder.encode(request.password()));
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict();
        }
        emailVerificationService.requestVerification(user);
        return SignupResponse.from(user);
    }
}
