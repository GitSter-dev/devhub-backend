package com.application.devhub.security;

import com.application.devhub.user.Role;
import com.application.devhub.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record AuthUser(UUID id, String username, String passwordHash, Role role, boolean verified)
        implements UserDetails {

    public static AuthUser from(User user) {
        return new AuthUser(user.getId(), user.getUsername(), user.getPasswordHash(), user.getRole(),
                user.isEmailVerified());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return verified;
    }
}
