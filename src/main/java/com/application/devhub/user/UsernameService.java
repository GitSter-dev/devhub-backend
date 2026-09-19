package com.application.devhub.user;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.api.ErrorCode;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsernameService {

    private final UserRepository userRepository;
    private final HeldUsernameRepository heldUsernames;
    private final UsernameProperties properties;
    private final Validator validator;

    @Transactional
    public CurrentUserResponse change(UUID userId, String requested) {
        User user = userRepository.findById(userId).orElseThrow(ApiException::notFound);
        String current = user.getUsername();
        if (current.equals(requested)) {
            return CurrentUserResponse.from(user, properties.changeCooldown());
        }
        Instant now = Instant.now();
        boolean caseOnly = current.equalsIgnoreCase(requested);
        Optional<HeldUsername> hold = heldUsernames.findActive(requested, now);
        boolean reclaiming = hold.map(held -> held.isHeldBy(userId)).orElse(false);
        if (hold.isPresent() && !reclaiming || !caseOnly && userRepository.existsByUsername(requested)) {
            throw ApiException.of(ErrorCode.USERNAME_TAKEN);
        }
        Instant availableAt = user.usernameChangeAvailableAt(properties.changeCooldown());
        if (!reclaiming && availableAt != null && availableAt.isAfter(now)) {
            throw ApiException.of(ErrorCode.USERNAME_CHANGE_TOO_SOON, availableAt.toString());
        }
        user.rename(requested);
        if (reclaiming) {
            heldUsernames.release(requested);
        }
        if (!caseOnly) {
            heldUsernames.hold(current, userId, now.plus(properties.hold()));
        }
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.of(ErrorCode.USERNAME_TAKEN);
        }
        return CurrentUserResponse.from(user, properties.changeCooldown());
    }

    @Transactional(readOnly = true)
    public UsernameAvailabilityResponse availability(UUID userId, String requested) {
        String candidate = requested == null ? "" : requested.strip();
        if (!validator.validateValue(ChangeUsernameRequest.class, "username", candidate).isEmpty()) {
            return UsernameAvailabilityResponse.unavailable(UsernameAvailabilityResponse.Reason.INVALID);
        }
        Optional<HeldUsername> hold = heldUsernames.findActive(candidate, Instant.now());
        if (hold.isPresent()) {
            return hold.get().isHeldBy(userId)
                    ? UsernameAvailabilityResponse.free()
                    : UsernameAvailabilityResponse.unavailable(UsernameAvailabilityResponse.Reason.TAKEN);
        }
        boolean mine = userRepository.findById(userId).map(user -> user.getUsername().equalsIgnoreCase(candidate))
                .orElse(false);
        return mine || !userRepository.existsByUsername(candidate)
                ? UsernameAvailabilityResponse.free()
                : UsernameAvailabilityResponse.unavailable(UsernameAvailabilityResponse.Reason.TAKEN);
    }

    @Transactional(readOnly = true)
    public boolean isHeld(String username) {
        return heldUsernames.findActive(username, Instant.now()).isPresent();
    }
}
