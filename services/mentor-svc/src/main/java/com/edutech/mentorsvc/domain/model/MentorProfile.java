package com.edutech.mentorsvc.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "mentor_profiles", schema = "mentor_schema")
public class MentorProfile {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "specializations", length = 500)
    private String specializations;

    @Column(name = "years_of_experience", nullable = false)
    private int yearsOfExperience;

    @Column(name = "hourly_rate", precision = 8, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "total_sessions", nullable = false)
    private int totalSessions;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime deletedAt;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "district", length = 100)
    private String district;

    protected MentorProfile() {
    }

    private MentorProfile(UUID id, UUID userId, String fullName, String email, String bio,
                          String specializations, int yearsOfExperience, BigDecimal hourlyRate,
                          boolean isAvailable) {
        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.bio = bio;
        this.specializations = specializations;
        this.yearsOfExperience = yearsOfExperience;
        this.hourlyRate = hourlyRate;
        this.isAvailable = isAvailable;
        this.averageRating = BigDecimal.ZERO;
        this.totalSessions = 0;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public static MentorProfile create(UUID userId, String fullName, String email, String bio,
                                       String specializations, int yearsOfExperience, BigDecimal hourlyRate) {
        return new MentorProfile(UUID.randomUUID(), userId, fullName, email, bio,
                specializations, yearsOfExperience, hourlyRate, true);
    }

    public void updateAvailability(boolean available) {
        this.isAvailable = available;
        this.updatedAt = OffsetDateTime.now();
    }

    public void incrementTotalSessions() {
        this.totalSessions++;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateAverageRating(BigDecimal newRating) {
        if (this.totalSessions == 0) {
            this.averageRating = newRating;
        } else {
            BigDecimal total = this.averageRating.multiply(BigDecimal.valueOf(this.totalSessions));
            this.averageRating = total.add(newRating)
                    .divide(BigDecimal.valueOf(this.totalSessions + 1L), 2, java.math.RoundingMode.HALF_UP);
        }
        this.updatedAt = OffsetDateTime.now();
    }

    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getBio() { return bio; }
    public String getSpecializations() { return specializations; }
    public int getYearsOfExperience() { return yearsOfExperience; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public boolean isAvailable() { return isAvailable; }
    public BigDecimal getAverageRating() { return averageRating; }
    public int getTotalSessions() { return totalSessions; }
    public long getVersion() { return version; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public String getGender() { return gender; }
    public String getDistrict() { return district; }

    public void updateGender(String gender) {
        this.gender = gender;
        this.updatedAt = OffsetDateTime.now();
    }

    public void update(String fullName, String bio, String specializations,
                       Integer yearsOfExperience, java.math.BigDecimal hourlyRate, String gender,
                       String district) {
        if (fullName != null && !fullName.isBlank()) this.fullName = fullName;
        if (bio != null) this.bio = bio;
        if (specializations != null) this.specializations = specializations;
        if (yearsOfExperience != null) this.yearsOfExperience = yearsOfExperience;
        if (hourlyRate != null) this.hourlyRate = hourlyRate;
        if (gender != null) this.gender = gender;
        if (district != null) this.district = district;
        this.updatedAt = OffsetDateTime.now();
    }
}
