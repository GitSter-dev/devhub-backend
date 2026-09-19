package com.application.devhub.security;

import com.application.devhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) {
        return userRepository.findByLogin(identifier)
                .map(AuthUser::from)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
