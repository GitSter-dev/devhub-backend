package com.application.devhub.otp;

import com.application.devhub.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class OneTimeCodeIssuer {

    private static final int CODE_BOUND = 1_000_000;

    private final SecureRandom random = new SecureRandom();
    private final OneTimeCodeRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final OneTimeCodeProperties properties;

    public boolean canIssue(User user, OneTimeCodePurpose purpose) {
        CodePolicy policy = properties.policyFor(purpose);
        Instant now = Instant.now();
        return repository.findByUserIdAndPurpose(user.getId(), purpose)
                .map(code -> !code.wasIssuedAfter(now.minus(policy.resendCooldown()))
                        && code.issuesInCurrentWindow(now) < policy.dailyLimit())
                .orElse(true);
    }

    public String issue(User user, OneTimeCodePurpose purpose) {
        String code = "%06d".formatted(random.nextInt(CODE_BOUND));
        Instant now = Instant.now();
        OneTimeCode stored = repository.findByUserIdAndPurpose(user.getId(), purpose)
                .orElseGet(() -> new OneTimeCode(user.getId(), purpose));
        stored.reissue(passwordEncoder.encode(code), now.plus(properties.policyFor(purpose).ttl()), now);
        repository.save(stored);
        return code;
    }
}
