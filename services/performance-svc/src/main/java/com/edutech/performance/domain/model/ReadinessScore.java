package com.edutech.performance.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "readiness_scores", schema = "performance_schema")
public class ReadinessScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "ers_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal ersScore;

    @Column(name = "syllabus_coverage_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal syllabusCoveragePercent;

    @Column(name = "mock_test_trend_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal mockTestTrendScore;

    @Column(name = "mastery_average", nullable = false, precision = 5, scale = 2)
    private BigDecimal masteryAverage;

    @Column(name = "time_management_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal timeManagementScore;

    @Column(name = "accuracy_consistency", nullable = false, precision = 5, scale = 2)
    private BigDecimal accuracyConsistency;

    @Column(name = "projected_rank")
    private Integer projectedRank;

    @Column(name = "projected_percentile", precision = 5, scale = 2)
    private BigDecimal projectedPercentile;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    private ReadinessScore() {
    }

    /**
     * Static factory. Computes ERS using weighted formula:
     * ERS = 0.25*syllabus + 0.30*mockTrend + 0.25*mastery + 0.10*timeMgmt + 0.10*accuracy
     */
    public static ReadinessScore compute(UUID studentId, UUID enrollmentId,
                                         BigDecimal syllabusCoverage,
                                         BigDecimal mockTrendScore,
                                         BigDecimal masteryAverage,
                                         BigDecimal timeManagementScore,
                                         BigDecimal accuracyConsistency) {
        BigDecimal ers = syllabusCoverage.multiply(new BigDecimal("0.25"))
                .add(mockTrendScore.multiply(new BigDecimal("0.30")))
                .add(masteryAverage.multiply(new BigDecimal("0.25")))
                .add(timeManagementScore.multiply(new BigDecimal("0.10")))
                .add(accuracyConsistency.multiply(new BigDecimal("0.10")));

        ReadinessScore score = new ReadinessScore();
        score.id = UUID.randomUUID();
        score.studentId = studentId;
        score.enrollmentId = enrollmentId;
        score.ersScore = ers.setScale(2, RoundingMode.HALF_UP);
        score.syllabusCoveragePercent = syllabusCoverage.setScale(2, RoundingMode.HALF_UP);
        score.mockTestTrendScore = mockTrendScore.setScale(2, RoundingMode.HALF_UP);
        score.masteryAverage = masteryAverage.setScale(2, RoundingMode.HALF_UP);
        score.timeManagementScore = timeManagementScore.setScale(2, RoundingMode.HALF_UP);
        score.accuracyConsistency = accuracyConsistency.setScale(2, RoundingMode.HALF_UP);
        score.computedAt = Instant.now();
        score.createdAt = Instant.now();
        return score;
    }

    public void assignProjectedRank(Integer rank, BigDecimal percentile) {
        this.projectedRank = rank;
        this.projectedPercentile = percentile;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public BigDecimal getErsScore() { return ersScore; }
    public BigDecimal getSyllabusCoveragePercent() { return syllabusCoveragePercent; }
    public BigDecimal getMockTestTrendScore() { return mockTestTrendScore; }
    public BigDecimal getMasteryAverage() { return masteryAverage; }
    public BigDecimal getTimeManagementScore() { return timeManagementScore; }
    public BigDecimal getAccuracyConsistency() { return accuracyConsistency; }
    public Integer getProjectedRank() { return projectedRank; }
    public BigDecimal getProjectedPercentile() { return projectedPercentile; }
    public Instant getComputedAt() { return computedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public Long getVersion() { return version; }
}
