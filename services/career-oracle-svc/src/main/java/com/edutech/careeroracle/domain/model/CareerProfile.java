package com.edutech.careeroracle.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "career_profiles", schema = "careeroracle_schema")
public class CareerProfile {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false, unique = true)
    private UUID studentId;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "academic_stream", nullable = false, length = 50)
    private String academicStream;

    @Column(name = "current_grade", nullable = false)
    private Integer currentGrade;

    @Column(name = "ers_score", precision = 5, scale = 2)
    private BigDecimal ersScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_career_stream", length = 50)
    private CareerStream preferredCareerStream;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected CareerProfile() {
    }

    private CareerProfile(UUID id, UUID studentId, UUID enrollmentId, String academicStream,
                          Integer currentGrade, BigDecimal ersScore, CareerStream preferredCareerStream,
                          OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.studentId = studentId;
        this.enrollmentId = enrollmentId;
        this.academicStream = academicStream;
        this.currentGrade = currentGrade;
        this.ersScore = ersScore;
        this.preferredCareerStream = preferredCareerStream;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CareerProfile create(UUID studentId, UUID enrollmentId, String academicStream,
                                       Integer currentGrade, BigDecimal ersScore,
                                       CareerStream preferredCareerStream) {
        OffsetDateTime now = OffsetDateTime.now();
        return new CareerProfile(
                UUID.randomUUID(),
                studentId,
                enrollmentId,
                academicStream,
                currentGrade,
                ersScore,
                preferredCareerStream,
                now,
                now
        );
    }

    public void update(String academicStream, Integer currentGrade, BigDecimal ersScore,
                       CareerStream preferredCareerStream) {
        this.academicStream = academicStream;
        this.currentGrade = currentGrade;
        this.ersScore = ersScore;
        this.preferredCareerStream = preferredCareerStream;
        this.updatedAt = OffsetDateTime.now();
    }

    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getEnrollmentId() {
        return enrollmentId;
    }

    public String getAcademicStream() {
        return academicStream;
    }

    public Integer getCurrentGrade() {
        return currentGrade;
    }

    public BigDecimal getErsScore() {
        return ersScore;
    }

    public CareerStream getPreferredCareerStream() {
        return preferredCareerStream;
    }

    public Long getVersion() {
        return version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }
}
