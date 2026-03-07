package com.edutech.performance.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only performance snapshot. No @Version (immutable).
 * Stored in a TimescaleDB hypertable partitioned by snapshot_at.
 */
@Entity
@Table(name = "performance_snapshots", schema = "performance_schema")
public class PerformanceSnapshot {

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

    /**
     * IRT ability estimate. Starts at 0.0, range -3.0 to +3.0.
     */
    @Column(name = "theta", nullable = false, precision = 5, scale = 4)
    private BigDecimal theta;

    @Column(name = "percentile", precision = 5, scale = 2)
    private BigDecimal percentile;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    @Column(name = "dropout_risk_score", nullable = false, precision = 4, scale = 3)
    private BigDecimal dropoutRiskScore;

    @Column(name = "total_study_minutes_today", nullable = false)
    private Integer totalStudyMinutesToday;

    @Column(name = "mock_tests_this_week", nullable = false)
    private Integer mockTestsThisWeek;

    /**
     * TimescaleDB partitions by this column.
     */
    @Column(name = "snapshot_at", nullable = false)
    private Instant snapshotAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    private PerformanceSnapshot() {
    }

    public static PerformanceSnapshot take(UUID studentId, UUID enrollmentId,
                                           BigDecimal ers, BigDecimal theta,
                                           BigDecimal percentile, RiskLevel riskLevel) {
        PerformanceSnapshot snapshot = new PerformanceSnapshot();
        snapshot.id = UUID.randomUUID();
        snapshot.studentId = studentId;
        snapshot.enrollmentId = enrollmentId;
        snapshot.ersScore = ers.setScale(2, RoundingMode.HALF_UP);
        snapshot.theta = theta.setScale(4, RoundingMode.HALF_UP);
        snapshot.percentile = percentile != null ? percentile.setScale(2, RoundingMode.HALF_UP) : null;
        snapshot.riskLevel = riskLevel;
        snapshot.dropoutRiskScore = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        snapshot.totalStudyMinutesToday = 0;
        snapshot.mockTestsThisWeek = 0;
        snapshot.snapshotAt = Instant.now();
        snapshot.createdAt = Instant.now();
        return snapshot;
    }

    public void setDropoutRiskScore(BigDecimal dropoutRiskScore) {
        this.dropoutRiskScore = dropoutRiskScore.setScale(3, RoundingMode.HALF_UP);
    }

    public void setTotalStudyMinutesToday(Integer totalStudyMinutesToday) {
        this.totalStudyMinutesToday = totalStudyMinutesToday;
    }

    public void setMockTestsThisWeek(Integer mockTestsThisWeek) {
        this.mockTestsThisWeek = mockTestsThisWeek;
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public BigDecimal getErsScore() { return ersScore; }
    public BigDecimal getTheta() { return theta; }
    public BigDecimal getPercentile() { return percentile; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public BigDecimal getDropoutRiskScore() { return dropoutRiskScore; }
    public Integer getTotalStudyMinutesToday() { return totalStudyMinutesToday; }
    public Integer getMockTestsThisWeek() { return mockTestsThisWeek; }
    public Instant getSnapshotAt() { return snapshotAt; }
    public Instant getCreatedAt() { return createdAt; }
}
