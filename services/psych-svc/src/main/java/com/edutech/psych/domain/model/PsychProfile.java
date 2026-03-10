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
@Table(name = "psych_profiles", schema = "psych_schema")
public class PsychProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "student_id", updatable = false, nullable = false)
    private UUID studentId;

    @Column(name = "center_id", updatable = false, nullable = false)
    private UUID centerId;

    @Column(name = "batch_id", updatable = false, nullable = false)
    private UUID batchId;

    @Column(nullable = false)
    private double openness;

    @Column(nullable = false)
    private double conscientiousness;

    @Column(nullable = false)
    private double extraversion;

    @Column(nullable = false)
    private double agreeableness;

    @Column(nullable = false)
    private double neuroticism;

    @Column(name = "riasec_code")
    private String riasecCode;

    @Lob
    @Column(name = "embedding_json", columnDefinition = "TEXT")
    private String embeddingJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfileStatus status;

    @Version
    private Long version;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private PsychProfile() {
    }

    public static PsychProfile create(UUID studentId, UUID centerId, UUID batchId) {
        PsychProfile profile = new PsychProfile();
        profile.id = UUID.randomUUID();
        profile.studentId = studentId;
        profile.centerId = centerId;
        profile.batchId = batchId;
        profile.openness = 0.0;
        profile.conscientiousness = 0.0;
        profile.extraversion = 0.0;
        profile.agreeableness = 0.0;
        profile.neuroticism = 0.0;
        profile.riasecCode = null;
        profile.embeddingJson = null;
        profile.status = ProfileStatus.DRAFT;
        profile.createdAt = Instant.now();
        profile.updatedAt = Instant.now();
        return profile;
    }

    public void activate() {
        if (this.status != ProfileStatus.DRAFT) {
            throw new IllegalStateException(
                    "Profile can only be activated from DRAFT status. Current status: " + this.status);
        }
        this.status = ProfileStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void archive() {
        if (this.status != ProfileStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Profile can only be archived from ACTIVE status. Current status: " + this.status);
        }
        this.status = ProfileStatus.ARCHIVED;
        this.updatedAt = Instant.now();
    }

    public void updateTraits(double openness, double conscientiousness, double extraversion,
                             double agreeableness, double neuroticism, String riasecCode) {
        if (this.status == ProfileStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot update traits on an ARCHIVED profile.");
        }
        this.openness = openness;
        this.conscientiousness = conscientiousness;
        this.extraversion = extraversion;
        this.agreeableness = agreeableness;
        this.neuroticism = neuroticism;
        this.riasecCode = riasecCode;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getCenterId() {
        return centerId;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public double getOpenness() {
        return openness;
    }

    public double getConscientiousness() {
        return conscientiousness;
    }

    public double getExtraversion() {
        return extraversion;
    }

    public double getAgreeableness() {
        return agreeableness;
    }

    public double getNeuroticism() {
        return neuroticism;
    }

    public String getRiasecCode() {
        return riasecCode;
    }

    public String getEmbeddingJson() {
        return embeddingJson;
    }

    public ProfileStatus getStatus() {
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
