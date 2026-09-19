package com.application.devhub.session;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.api.ErrorCode;
import com.application.devhub.ratelimit.RateLimitPolicy;
import com.application.devhub.ratelimit.RateLimiter;
import com.application.devhub.security.AuthUser;
import com.application.devhub.security.TokenService;
import com.application.devhub.user.AccountAccess;
import com.application.devhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final RateLimiter rateLimiter;
    private final AccountAccess accountAccess;

    public TokenPair login(String identifier, String password) {
        String accountKey = identifier.toLowerCase(Locale.ROOT);
        rateLimiter.ensureAvailable(RateLimitPolicy.LOGIN_FAILURES, accountKey);
        AuthUser user = authenticate(identifier, password, accountKey);
        userRepository.findById(user.id()).ifPresent(accountAccess::ensureCanSignIn);
        IssuedRefreshToken refreshToken = refreshTokenService.startSession(user.id());
        return TokenPair.of(tokenService.issueAccessToken(user, refreshToken.familyId()), refreshToken);
    }

    public TokenPair refresh(String rawRefreshToken, String idempotencyKey) {
        Rotation rotation = refreshTokenService.rotate(rawRefreshToken, idempotencyKey);
        AuthUser user = userRepository.findById(rotation.userId())
                .filter(accountAccess::canContinueSession)
                .map(AuthUser::from)
                .orElseThrow(() -> ApiException.of(ErrorCode.INVALID_REFRESH_TOKEN));
        return TokenPair.of(tokenService.issueAccessToken(user, rotation.refreshToken().familyId()), rotation.refreshToken());
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeFamilyOf(rawRefreshToken);
    }

    private AuthUser authenticate(String identifier, String password, String accountKey) {
        try {
            return (AuthUser) authenticationManager
                    .authenticate(UsernamePasswordAuthenticationToken.unauthenticated(identifier, password))
                    .getPrincipal();
        } catch (DisabledException e) {
            throw ApiException.of(ErrorCode.EMAIL_NOT_VERIFIED);
        } catch (AuthenticationException e) {
            rateLimiter.recordHit(RateLimitPolicy.LOGIN_FAILURES, accountKey);
            throw ApiException.of(ErrorCode.INVALID_CREDENTIALS);
        }
    }
}
