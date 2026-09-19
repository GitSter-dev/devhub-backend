package com.application.devhub.security;

import com.application.devhub.IntegrationTest;
import com.application.devhub.user.User;
import com.application.devhub.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationManagerIntegrationTest extends IntegrationTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User verified;

    @BeforeEach
    void setUp() {
        verified = new User("AdaLovelace", "Ada Lovelace", "ada@devhub.dev", passwordEncoder.encode(PASSWORD));
        verified.markEmailVerified();
        verified = userRepository.save(verified);
        userRepository.save(new User("grace", "Grace Hopper", "grace@devhub.dev", passwordEncoder.encode(PASSWORD)));
    }

    @Test
    void authenticatesWithEmail() {
        Authentication result = authenticate("ada@devhub.dev", PASSWORD);
        assertThat(result.isAuthenticated()).isTrue();
        assertThat(((AuthUser) result.getPrincipal()).id()).isEqualTo(verified.getId());
    }

    @Test
    void authenticatesWithUsernameIgnoringCase() {
        Authentication result = authenticate("adalovelace", PASSWORD);
        assertThat(result.isAuthenticated()).isTrue();
    }

    @Test
    void rejectsWrongPassword() {
        assertThatThrownBy(() -> authenticate("ada@devhub.dev", "wrong"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void unknownUserLooksLikeWrongPassword() {
        assertThatThrownBy(() -> authenticate("nobody@devhub.dev", PASSWORD))
                .isExactlyInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rejectsUnverifiedUser() {
        assertThatThrownBy(() -> authenticate("grace", PASSWORD))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void unverifiedUserWithWrongPasswordGetsBadCredentials() {
        assertThatThrownBy(() -> authenticate("grace", "wrong"))
                .isExactlyInstanceOf(BadCredentialsException.class);
    }

    private Authentication authenticate(String identifier, String password) {
        return authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(identifier, password));
    }
}
