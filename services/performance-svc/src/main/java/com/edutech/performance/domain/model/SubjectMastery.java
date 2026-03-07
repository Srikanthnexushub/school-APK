package com.edutech.performance.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "subject_mastery", schema = "performance_schema")
public class SubjectMastery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "mastery_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal masteryPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "mastery_level", nullable = false)
    private MasteryLevel masteryLevel;

    @Column(name = "velocity_per_week", precision = 7, scale = 4)
    private BigDecimal velocityPerWeek;

    @Column(name = "total_topics", nullable = false)
    private Integer totalTopics;

    @Column(name = "mastered_topics", nullable = false)
    private Integer masteredTopics;

    @Column(name = "last_updated_at")
    private Instant lastUpdatedAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;

    private SubjectMastery() {
    }

    public static SubjectMastery create(UUID studentId, UUID enrollmentId, String subject) {
        SubjectMastery mastery = new SubjectMastery();
        mastery.id = UUID.randomUUID();
        mastery.studentId = studentId;
        mastery.enrollmentId = enrollmentId;
        mastery.subject = subject;
        mastery.masteryPercent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        mastery.masteryLevel = MasteryLevel.BEGINNER;
        mastery.velocityPerWeek = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        mastery.totalTopics = 0;
        mastery.masteredTopics = 0;
        mastery.lastUpdatedAt = Instant.now();
        mastery.createdAt = Instant.now();
        mastery.updatedAt = Instant.now();
        return mastery;
    }

    public void updateMastery(BigDecimal newMasteryPercent) {
        BigDecimal previousPercent = this.masteryPercent;
        Instant previousUpdate = this.lastUpdatedAt != null ? this.lastUpdatedAt : this.createdAt;

        this.masteryPercent = newMasteryPercent.setScale(2, RoundingMode.HALF_UP);
        this.masteryLevel = MasteryLevel.from(newMasteryPercent);

        long weeksBetween = ChronoUnit.WEEKS.between(previousUpdate, Instant.now());
        if (weeksBetween > 0) {
            BigDecimal delta = newMasteryPercent.subtract(previousPercent);
            this.velocityPerWeek = delta.divide(BigDecimal.valueOf(weeksBetween), 4, RoundingMode.HALF_UP);
        } else {
            this.velocityPerWeek = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        this.lastUpdatedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void updateTopicCounts(Integer totalTopics, Integer masteredTopics) {
        this.totalTopics = totalTopics;
        this.masteredTopics = masteredTopics;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public String getSubject() { return subject; }
    public BigDecimal getMasteryPercent() { return masteryPercent; }
    public MasteryLevel getMasteryLevel() { return masteryLevel; }
    public BigDecimal getVelocityPerWeek() { return velocityPerWeek; }
    public Integer getTotalTopics() { return totalTopics; }
    public Integer getMasteredTopics() { return masteredTopics; }
    public Instant getLastUpdatedAt() { return lastUpdatedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
