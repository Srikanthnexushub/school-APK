// src/main/java/com/edutech/parent/domain/model/ParentProfile.java
package com.edutech.parent.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "parent_profiles", schema = "parent_schema")
public class ParentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column
    private String phone;

    @Column(nullable = false)
    private boolean verified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParentStatus status;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private ParentProfile() {}

    public static ParentProfile create(UUID userId, String name, String phone) {
        ParentProfile profile = new ParentProfile();
        profile.userId = userId;
        profile.name = name;
        profile.phone = phone;
        profile.verified = false;
        profile.status = ParentStatus.ACTIVE;
        profile.createdAt = Instant.now();
        profile.updatedAt = profile.createdAt;
        return profile;
    }

    public void update(String name, String phone) {
        if (name != null) {
            this.name = name;
        }
        if (phone != null) {
            this.phone = phone;
        }
        this.updatedAt = Instant.now();
    }

    public void verify() {
        this.verified = true;
        this.updatedAt = Instant.now();
    }

    public void suspend() {
        if (this.status != ParentStatus.ACTIVE) {
            throw new IllegalStateException("Cannot suspend a parent profile that is not ACTIVE. Current status: " + this.status);
        }
        this.status = ParentStatus.SUSPENDED;
        this.updatedAt = Instant.now();
    }

    public void reactivate() {
        if (this.status != ParentStatus.SUSPENDED) {
            throw new IllegalStateException("Cannot reactivate a parent profile that is not SUSPENDED. Current status: " + this.status);
        }
        this.status = ParentStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isVerified() {
        return verified;
    }

    public ParentStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
