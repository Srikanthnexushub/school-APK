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

    @Column
    private String email;

    @Column
    private String address;

    @Column
    private String city;

    @Column
    private String state;

    @Column
    private String district;

    @Column
    private String country;

    @Column
    private String pincode;

    @Column(name = "relationship_type")
    private String relationshipType;

    @Column
    private String occupation;

    @Column
    private String gender;

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

    public static ParentProfile create(UUID userId, String name, String phone,
                                       String email, String address, String city,
                                       String state, String pincode, String relationshipType,
                                       String occupation) {
        return create(userId, name, phone, email, address, city, null, state, null, pincode, relationshipType, occupation, null);
    }

    public static ParentProfile create(UUID userId, String name, String phone,
                                       String email, String address, String city,
                                       String state, String pincode, String relationshipType,
                                       String occupation, String gender) {
        return create(userId, name, phone, email, address, city, null, state, null, pincode, relationshipType, occupation, gender);
    }

    public static ParentProfile create(UUID userId, String name, String phone,
                                       String email, String address, String city,
                                       String district, String state, String country,
                                       String pincode, String relationshipType,
                                       String occupation, String gender) {
        ParentProfile profile = new ParentProfile();
        profile.userId = userId;
        profile.name = name;
        profile.phone = phone;
        profile.email = email;
        profile.address = address;
        profile.city = city;
        profile.district = district;
        profile.state = state;
        profile.country = country;
        profile.pincode = pincode;
        profile.relationshipType = (relationshipType != null) ? relationshipType : "PARENT";
        profile.occupation = occupation;
        profile.gender = gender;
        profile.verified = false;
        profile.status = ParentStatus.ACTIVE;
        profile.createdAt = Instant.now();
        profile.updatedAt = profile.createdAt;
        return profile;
    }

    public void update(String name, String phone, String email, String address,
                       String city, String state, String pincode, String relationshipType,
                       String occupation) {
        update(name, phone, email, address, city, null, state, null, pincode, relationshipType, occupation, null);
    }

    public void update(String name, String phone, String email, String address,
                       String city, String state, String pincode, String relationshipType,
                       String occupation, String gender) {
        update(name, phone, email, address, city, null, state, null, pincode, relationshipType, occupation, gender);
    }

    public void update(String name, String phone, String email, String address,
                       String city, String district, String state, String country,
                       String pincode, String relationshipType, String occupation, String gender) {
        if (name != null) {
            this.name = name;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (email != null) {
            this.email = email;
        }
        if (address != null) {
            this.address = address;
        }
        if (city != null) {
            this.city = city;
        }
        if (district != null) {
            this.district = district;
        }
        if (state != null) {
            this.state = state;
        }
        if (country != null) {
            this.country = country;
        }
        if (pincode != null) {
            this.pincode = pincode;
        }
        if (relationshipType != null) {
            this.relationshipType = relationshipType;
        }
        if (occupation != null) {
            this.occupation = occupation;
        }
        if (gender != null) {
            this.gender = gender;
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

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getDistrict() {
        return district;
    }

    public String getCountry() {
        return country;
    }

    public String getPincode() {
        return pincode;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public String getOccupation() {
        return occupation;
    }

    public String getGender() {
        return gender;
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
