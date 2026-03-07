package com.edutech.psych.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "career_mapping", schema = "psych_schema")
public class CareerMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "profile_id", updatable = false, nullable = false)
    private UUID profileId;

    @Column(name = "student_id", updatable = false, nullable = false)
    private UUID studentId;

    @Column(name = "center_id", updatable = false, nullable = false)
    private UUID centerId;

    @Column(name = "requested_at", updatable = false, nullable = false)
    private Instant requestedAt;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @Lob
    @Column(name = "top_careers", columnDefinition = "TEXT")
    private String topCareers;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String reasoning;

    @Column(name = "model_version")
    private String modelVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CareerMappingStatus status;

    @Version
    private Long version;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private CareerMapping() {
    }

    public static CareerMapping create(UUID profileId, UUID studentId, UUID centerId) {
        CareerMapping mapping = new CareerMapping();
        mapping.id = UUID.randomUUID();
        mapping.profileId = profileId;
        mapping.studentId = studentId;
        mapping.centerId = centerId;
        mapping.requestedAt = Instant.now();
        mapping.status = CareerMappingStatus.PENDING;
        mapping.createdAt = Instant.now();
        mapping.updatedAt = Instant.now();
        return mapping;
    }

    public void complete(String topCareers, String reasoning, String modelVersion) {
        if (this.status != CareerMappingStatus.PENDING) {
            throw new IllegalStateException(
                    "Career mapping can only be completed from PENDING status. Current status: " + this.status);
        }
        this.status = CareerMappingStatus.GENERATED;
        this.topCareers = topCareers;
        this.reasoning = reasoning;
        this.modelVersion = modelVersion;
        this.generatedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void fail() {
        if (this.status != CareerMappingStatus.PENDING) {
            throw new IllegalStateException(
                    "Career mapping can only be failed from PENDING status. Current status: " + this.status);
        }
        this.status = CareerMappingStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProfileId() {
        return profileId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getCenterId() {
        return centerId;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public String getTopCareers() {
        return topCareers;
    }

    public String getReasoning() {
        return reasoning;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public CareerMappingStatus getStatus() {
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
