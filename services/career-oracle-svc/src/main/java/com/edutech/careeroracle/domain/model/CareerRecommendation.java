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
@Table(name = "career_recommendations", schema = "careeroracle_schema")
public class CareerRecommendation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "career_stream", nullable = false, length = 50)
    private CareerStream careerStream;

    @Column(name = "fit_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal fitScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", nullable = false, length = 20)
    private ConfidenceLevel confidenceLevel;

    @Column(name = "rationale", nullable = false, columnDefinition = "TEXT")
    private String rationale;

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    @Column(name = "valid_until")
    private OffsetDateTime validUntil;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected CareerRecommendation() {
    }

    private CareerRecommendation(UUID id, UUID studentId, CareerStream careerStream,
                                  BigDecimal fitScore, ConfidenceLevel confidenceLevel,
                                  String rationale, Integer rankOrder,
                                  OffsetDateTime generatedAt, OffsetDateTime validUntil,
                                  Boolean isActive) {
        this.id = id;
        this.studentId = studentId;
        this.careerStream = careerStream;
        this.fitScore = fitScore;
        this.confidenceLevel = confidenceLevel;
        this.rationale = rationale;
        this.rankOrder = rankOrder;
        this.generatedAt = generatedAt;
        this.validUntil = validUntil;
        this.isActive = isActive;
    }

    public static CareerRecommendation create(UUID studentId, CareerStream careerStream,
                                               BigDecimal fitScore, ConfidenceLevel confidenceLevel,
                                               String rationale, Integer rankOrder,
                                               OffsetDateTime validUntil) {
        return new CareerRecommendation(
                UUID.randomUUID(),
                studentId,
                careerStream,
                fitScore,
                confidenceLevel,
                rationale,
                rankOrder,
                OffsetDateTime.now(),
                validUntil,
                true
        );
    }

    public void deactivate() {
        this.isActive = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public CareerStream getCareerStream() {
        return careerStream;
    }

    public BigDecimal getFitScore() {
        return fitScore;
    }

    public ConfidenceLevel getConfidenceLevel() {
        return confidenceLevel;
    }

    public String getRationale() {
        return rationale;
    }

    public Integer getRankOrder() {
        return rankOrder;
    }

    public OffsetDateTime getGeneratedAt() {
        return generatedAt;
    }

    public OffsetDateTime getValidUntil() {
        return validUntil;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public Long getVersion() {
        return version;
    }
}
