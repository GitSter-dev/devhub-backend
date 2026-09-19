package com.application.devhub.user;

import com.application.devhub.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(nullable = false, length = 30)
    private String username;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "setup_completed_at")
    private Instant setupCompletedAt;

    @Column(length = 160)
    private String bio;

    @Column(name = "github_username", length = 39)
    private String githubUsername;

    @Column(name = "website_url", length = 200)
    private String websiteUrl;

    @Column(name = "username_changed_at")
    private Instant usernameChangedAt;

    @Column(name = "suspended_until")
    private Instant suspendedUntil;

    @Column(name = "banned_at")
    private Instant bannedAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public User(String username, String displayName, String email, String passwordHash) {
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = Role.USER;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void markEmailVerified() {
        this.emailVerifiedAt = Instant.now();
    }

    public boolean isSetupCompleted() {
        return setupCompletedAt != null;
    }

    public void completeSetup() {
        if (setupCompletedAt == null) {
            this.setupCompletedAt = Instant.now();
        }
    }

    public void updateProfile(ProfileDetails details) {
        this.displayName = details.displayName();
        this.bio = details.bio();
        this.githubUsername = details.githubUsername();
        this.websiteUrl = details.websiteUrl();
    }

    public Instant usernameChangeAvailableAt(Duration cooldown) {
        return usernameChangedAt == null ? null : usernameChangedAt.plus(cooldown);
    }

    public void rename(String username) {
        this.username = username;
        this.usernameChangedAt = Instant.now();
    }

    public void anonymize(String anonymousUsername, String anonymousEmail, String displayName) {
        this.username = anonymousUsername;
        this.email = anonymousEmail;
        this.displayName = displayName;
        this.passwordHash = "";
        this.bio = null;
        this.githubUsername = null;
        this.websiteUrl = null;
        this.deletedAt = Instant.now();
    }

    public void promoteToAdmin() {
        this.role = Role.ADMIN;
    }

    public boolean isBanned() {
        return bannedAt != null;
    }

    public boolean isSuspendedAt(Instant now) {
        return suspendedUntil != null && suspendedUntil.isAfter(now);
    }

    public boolean isDeactivated() {
        return deactivatedAt != null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isVisible() {
        return isEmailVerified() && !isBanned() && !isDeactivated();
    }

    public void suspendUntil(Instant until) {
        this.suspendedUntil = until;
    }

    public void ban() {
        if (bannedAt == null) {
            this.bannedAt = Instant.now();
        }
    }

    public void reinstate() {
        this.suspendedUntil = null;
        this.bannedAt = null;
    }

    public void deactivate() {
        if (deactivatedAt == null) {
            this.deactivatedAt = Instant.now();
        }
    }

    public void reactivate() {
        this.deactivatedAt = null;
    }
}
