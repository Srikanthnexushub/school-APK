// src/main/java/com/edutech/center/domain/model/CoachingCenter.java
package com.edutech.center.domain.model;

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
 * Aggregate root representing a coaching center (tenant boundary).
 * Row-level security in PostgreSQL enforces center isolation.
 * No public setters — state changes via named domain methods only.
 */
@Entity
@Table(
    name = "centers",
    schema = "center_schema",
    uniqueConstraints = @UniqueConstraint(name = "uq_centers_code", columnNames = "code")
)
public class CoachingCenter {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 30)
    private String code;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 10)
    private String pincode;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 500)
    private String website;

    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CenterStatus status;

    @Column(name = "admin_user_id")
    private UUID adminUserId;

    @Column(name = "registration_source", nullable = false, length = 30)
    private String registrationSource;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    protected CoachingCenter() {}

    private CoachingCenter(UUID id, String name, String code, String address,
                           String city, String state, String pincode,
                           String phone, String email, String website,
                           String logoUrl, UUID ownerId, String registrationSource) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.address = address;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.phone = phone;
        this.email = email;
        this.website = website;
        this.logoUrl = logoUrl;
        this.status = CenterStatus.ACTIVE;
        this.ownerId = ownerId;
        this.registrationSource = registrationSource;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /** SUPER_ADMIN creates a center directly — immediately ACTIVE, code required. */
    public static CoachingCenter create(String name, String code, String address,
                                        String city, String state, String pincode,
                                        String phone, String email, String website,
                                        String logoUrl, UUID ownerId) {
        return new CoachingCenter(UUID.randomUUID(), name, code, address,
                city, state, pincode, phone, email, website, logoUrl, ownerId, "ADMIN_CREATED");
    }

    /** CENTER_ADMIN self-registers their institution — immediately ACTIVE, code assigned later by SUPER_ADMIN. */
    public static CoachingCenter selfRegister(String name, String city, String phone,
                                              String email, String address, String state,
                                              String pincode, UUID ownerId) {
        return new CoachingCenter(UUID.randomUUID(), name, null,
                address != null && !address.isBlank() ? address : "-",
                city,
                state != null && !state.isBlank() ? state : "-",
                pincode != null && !pincode.isBlank() ? pincode : "000000",
                phone, email, null, null, ownerId, "SELF_REGISTERED");
    }

    public void update(String name, String address, String city, String state,
                       String pincode, String phone, String email,
                       String website, String logoUrl) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.phone = phone;
        this.email = email;
        this.website = website;
        this.logoUrl = logoUrl;
        this.updatedAt = Instant.now();
    }

    public void suspend() {
        if (this.status == CenterStatus.CLOSED) {
            throw new IllegalStateException("Cannot suspend a closed center");
        }
        this.status = CenterStatus.SUSPENDED;
        this.updatedAt = Instant.now();
    }

    public void close() {
        this.status = CenterStatus.CLOSED;
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void reactivate() {
        if (this.status == CenterStatus.CLOSED) {
            throw new IllegalStateException("Cannot reactivate a closed center");
        }
        this.status = CenterStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() { return this.status == CenterStatus.ACTIVE && this.deletedAt == null; }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPincode() { return pincode; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getWebsite() { return website; }
    public String getLogoUrl() { return logoUrl; }
    public CenterStatus getStatus() { return status; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getAdminUserId() { return adminUserId; }
    public String getRegistrationSource() { return registrationSource; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public Long getVersion() { return version; }
}
