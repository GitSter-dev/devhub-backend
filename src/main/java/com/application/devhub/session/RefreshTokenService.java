package com.application.devhub.session;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.api.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();
    private final RefreshTokenRepository repository;
    private final RefreshTokenHasher hasher;
    private final SessionProperties properties;
    private final ApplicationEventPublisher events;

    @Transactional
    public IssuedRefreshToken startSession(UUID userId) {
        repository.revokeAllForUser(userId, Instant.now(), RevocationReason.REPLACED);
        events.publishEvent(new UserSessionsEnded(userId, RevocationReason.REPLACED));
        return issue(userId, UUID.randomUUID(), null, null);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Rotation rotate(String rawToken, String idempotencyKey) {
        RefreshToken token = repository.findByTokenHash(hasher.hash(rawToken))
                .orElseThrow(() -> ApiException.of(ErrorCode.INVALID_REFRESH_TOKEN));
        if (token.isRevoked()) {
            throw ApiException.of(token.getRevocationReason() == RevocationReason.REPLACED
                    ? ErrorCode.SESSION_REPLACED
                    : ErrorCode.INVALID_REFRESH_TOKEN);
        }
        String requestHash = idempotencyKey == null ? null : hasher.hash(idempotencyKey);
        if (token.wasUsed()) {
            return replay(token, requestHash);
        }
        if (token.isExpired()) {
            throw ApiException.of(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        token.markUsed();
        return new Rotation(token.getUserId(), issue(token.getUserId(), token.getFamilyId(), token.getId(), requestHash));
    }

    @Transactional
    public void revokeAllFor(UUID userId, RevocationReason reason) {
        repository.revokeAllForUser(userId, Instant.now(), reason);
        events.publishEvent(new UserSessionsEnded(userId, reason));
    }

    @Transactional(readOnly = true)
    public boolean isSessionActive(UUID familyId) {
        return repository.existsActiveInFamily(familyId, Instant.now());
    }

    @Transactional
    public void revokeFamilyOf(String rawToken) {
        repository.findByTokenHash(hasher.hash(rawToken))
                .ifPresent(token -> endFamily(token.getFamilyId(), RevocationReason.LOGOUT));
    }

    private Rotation replay(RefreshToken token, String requestHash) {
        Optional<RefreshToken> successor = replayableSuccessor(token, requestHash);
        if (successor.isEmpty()) {
            endFamily(token.getFamilyId(), RevocationReason.REUSE_DETECTED);
            throw ApiException.of(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        successor.get().revoke(RevocationReason.SUPERSEDED);
        return new Rotation(token.getUserId(), issue(token.getUserId(), token.getFamilyId(), token.getId(), requestHash));
    }

    private void endFamily(UUID familyId, RevocationReason reason) {
        repository.revokeFamily(familyId, Instant.now(), reason);
        events.publishEvent(new SessionEnded(familyId, reason));
    }

    private Optional<RefreshToken> replayableSuccessor(RefreshToken token, String requestHash) {
        if (requestHash == null || !token.wasUsedAfter(Instant.now().minus(properties.refreshReplayWindow()))) {
            return Optional.empty();
        }
        return repository.findFirstByParentIdOrderByCreatedAtDesc(token.getId())
                .filter(successor -> !successor.wasUsed() && !successor.isRevoked())
                .filter(successor -> successor.wasIssuedFor(requestHash));
    }

    private IssuedRefreshToken issue(UUID userId, UUID familyId, UUID parentId, String requestHash) {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plus(properties.refreshTokenTtl());
        repository.save(new RefreshToken(userId, familyId, parentId, requestHash, hasher.hash(rawToken), expiresAt));
        return new IssuedRefreshToken(rawToken, expiresAt, familyId);
    }
}
