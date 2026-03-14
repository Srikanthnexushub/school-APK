// src/main/java/com/edutech/auth/domain/model/User.java
package com.edutech.auth.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * User aggregate root.
 *
 * Design rules enforced here:
 * - No public setters — state changes only through named domain methods.
 * - Immutable fields (id, email, createdAt) use @Column(updatable = false).
 * - Soft delete via deletedAt — no physical DELETE ever executed.
 * - Optimistic locking via @Version to prevent concurrent writes.
 */
@Entity
@Table(
    name = "users",
    schema = "auth_schema",
    uniqueConstraints = @UniqueConstraint(name = "uq_users_email", columnNames = "email")
)
public class User {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(updatable = false, nullable = false)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserStatus status;

    @Column(name = "center_id")
    private UUID centerId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    @Column(name = "parent_email")
    private String parentEmail;

    @Column(name = "provider", length = 30)
    private String provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    @Column(name = "totp_secret", length = 64)
    private String totpSecret;

    // Required by JPA — not for application use
    protected User() {}

    private User(UUID id, String email, String passwordHash, Role role,
                 UserStatus status, UUID centerId, String firstName,
                 String lastName, String phoneNumber, String parentEmail) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.centerId = centerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.parentEmail = parentEmail;
        this.emailVerified = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Factory methods — only way to create a new User
    // -------------------------------------------------------------------------

    public static User create(String email, String passwordHash, Role role,
                              UUID centerId, String firstName,
                              String lastName, String phoneNumber) {
        return new User(
            UUID.randomUUID(), email, passwordHash, role,
            UserStatus.PENDING_VERIFICATION, centerId,
            firstName, lastName, phoneNumber, null
        );
    }

    public static User create(String email, String passwordHash, Role role,
                              UUID centerId, String firstName,
                              String lastName, String phoneNumber, String parentEmail) {
        return new User(
            UUID.randomUUID(), email, passwordHash, role,
            UserStatus.PENDING_VERIFICATION, centerId,
            firstName, lastName, phoneNumber, parentEmail
        );
    }

    /**
     * Factory for OAuth2 sign-in (Google, etc.).
     * These users are immediately ACTIVE (email already verified by OAuth provider).
     */
    public static User createFromOAuth(String email, String firstName, String lastName,
                                       String provider, String providerId,
                                       Role role, UUID centerId) {
        User u = new User(
            UUID.randomUUID(), email, null, role,
            UserStatus.ACTIVE, centerId, firstName, lastName, null, null
        );
        u.provider = provider;
        u.providerId = providerId;
        u.emailVerified = true;
        return u;
    }

    // -------------------------------------------------------------------------
    // Domain methods — state transitions with precondition guards
    // -------------------------------------------------------------------------

    public void activate() {
        if (this.status != UserStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException(
                "Cannot activate user in status: " + this.status);
        }
        this.status = UserStatus.ACTIVE;
        this.emailVerified = true;
        this.updatedAt = Instant.now();
    }

    public void lock(String reason) {
        if (this.status == UserStatus.DEACTIVATED) {
            throw new IllegalStateException("Cannot lock a deactivated user");
        }
        this.status = UserStatus.LOCKED;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = UserStatus.DEACTIVATED;
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void updateName(String firstName, String lastName) {
        if (firstName != null && !firstName.isBlank()) this.firstName = firstName;
        if (lastName != null && !lastName.isBlank()) this.lastName = lastName;
        this.updatedAt = Instant.now();
    }

    public void updatePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.updatedAt = Instant.now();
    }

    public void enableMfa(String totpSecret) {
        this.totpSecret = totpSecret;
        this.mfaEnabled = true;
        this.updatedAt = Instant.now();
    }

    public void disableMfa() {
        this.totpSecret = null;
        this.mfaEnabled = false;
        this.updatedAt = Instant.now();
    }

    /** Links an OAuth provider to an existing email/password account. */
    public void linkOAuthProvider(String provider, String providerId) {
        this.provider = provider;
        this.providerId = providerId;
        this.emailVerified = true;
        this.updatedAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Query methods
    // -------------------------------------------------------------------------

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE && this.deletedAt == null;
    }

    public boolean isLocked() {
        return this.status == UserStatus.LOCKED;
    }

    public boolean isPendingVerification() {
        return this.status == UserStatus.PENDING_VERIFICATION;
    }

    public boolean isMfaEnabled() {
        return this.mfaEnabled;
    }

    // -------------------------------------------------------------------------
    // Read-only accessors — no setters exposed
    // -------------------------------------------------------------------------

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public UUID getCenterId() { return centerId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhoneNumber() { return phoneNumber; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public Long getVersion() { return version; }
    public String getParentEmail() { return parentEmail; }
    public String getProvider() { return provider; }
    public String getProviderId() { return providerId; }
    public boolean isEmailVerified() { return emailVerified; }
    public String getTotpSecret() { return totpSecret; }
}
