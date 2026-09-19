package com.application.devhub.otp;

import com.application.devhub.user.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OneTimeCodeVerifier {

    private final OneTimeCodeRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final OneTimeCodeProperties properties;
    private final String dummyCodeHash;

    public OneTimeCodeVerifier(OneTimeCodeRepository repository, PasswordEncoder passwordEncoder,
                               OneTimeCodeProperties properties) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.dummyCodeHash = passwordEncoder.encode("000000");
    }

    public boolean verify(Optional<User> user, OneTimeCodePurpose purpose, String code) {
        Optional<OneTimeCode> stored = user
                .flatMap(candidate -> repository.findByUserIdAndPurpose(candidate.getId(), purpose))
                .filter(OneTimeCode::isActive);

        if (stored.isEmpty()) {
            passwordEncoder.matches(code, dummyCodeHash);
            return false;
        }

        OneTimeCode oneTimeCode = stored.get();
        if (!passwordEncoder.matches(code, oneTimeCode.getCodeHash())) {
            if (oneTimeCode.registerFailedAttempt() >= properties.policyFor(purpose).maxAttempts()) {
                oneTimeCode.invalidate();
            }
            return false;
        }

        oneTimeCode.invalidate();
        return true;
    }
}
