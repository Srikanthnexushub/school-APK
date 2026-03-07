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
@Table(name = "college_predictions", schema = "careeroracle_schema")
public class CollegePrediction {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "college_name", nullable = false, length = 255)
    private String collegeName;

    @Column(name = "course_name", nullable = false, length = 255)
    private String courseName;

    @Enumerated(EnumType.STRING)
    @Column(name = "college_tier", nullable = false, length = 20)
    private CollegeTier collegeTier;

    @Column(name = "predicted_cutoff", precision = 6, scale = 2)
    private BigDecimal predictedCutoff;

    @Column(name = "student_predicted_score", precision = 6, scale = 2)
    private BigDecimal studentPredictedScore;

    @Column(name = "admission_probability", precision = 5, scale = 2)
    private BigDecimal admissionProbability;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected CollegePrediction() {
    }

    private CollegePrediction(UUID id, UUID studentId, UUID enrollmentId, String collegeName,
                               String courseName, CollegeTier collegeTier,
                               BigDecimal predictedCutoff, BigDecimal studentPredictedScore,
                               BigDecimal admissionProbability, OffsetDateTime generatedAt) {
        this.id = id;
        this.studentId = studentId;
        this.enrollmentId = enrollmentId;
        this.collegeName = collegeName;
        this.courseName = courseName;
        this.collegeTier = collegeTier;
        this.predictedCutoff = predictedCutoff;
        this.studentPredictedScore = studentPredictedScore;
        this.admissionProbability = admissionProbability;
        this.generatedAt = generatedAt;
    }

    public static CollegePrediction create(UUID studentId, UUID enrollmentId, String collegeName,
                                            String courseName, CollegeTier collegeTier,
                                            BigDecimal predictedCutoff, BigDecimal studentPredictedScore,
                                            BigDecimal admissionProbability) {
        return new CollegePrediction(
                UUID.randomUUID(),
                studentId,
                enrollmentId,
                collegeName,
                courseName,
                collegeTier,
                predictedCutoff,
                studentPredictedScore,
                admissionProbability,
                OffsetDateTime.now()
        );
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

    public String getCollegeName() {
        return collegeName;
    }

    public String getCourseName() {
        return courseName;
    }

    public CollegeTier getCollegeTier() {
        return collegeTier;
    }

    public BigDecimal getPredictedCutoff() {
        return predictedCutoff;
    }

    public BigDecimal getStudentPredictedScore() {
        return studentPredictedScore;
    }

    public BigDecimal getAdmissionProbability() {
        return admissionProbability;
    }

    public OffsetDateTime getGeneratedAt() {
        return generatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
