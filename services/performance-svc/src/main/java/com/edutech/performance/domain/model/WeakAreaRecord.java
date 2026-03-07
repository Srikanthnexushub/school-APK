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
import java.util.UUID;

@Entity
@Table(name = "weak_area_records", schema = "performance_schema")
public class WeakAreaRecord {

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

    @Column(name = "topic_name", nullable = false)
    private String topicName;

    @Column(name = "chapter_name")
    private String chapterName;

    @Column(name = "mastery_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal masteryPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_error_type", nullable = false)
    private ErrorType primaryErrorType;

    @Column(name = "incorrect_attempts", nullable = false)
    private Integer incorrectAttempts;

    @Column(name = "total_attempts", nullable = false)
    private Integer totalAttempts;

    @Column(name = "prerequisites_weak", nullable = false)
    private Boolean prerequisitesWeak;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    private WeakAreaRecord() {
    }

    public static WeakAreaRecord detect(UUID studentId, UUID enrollmentId,
                                        String subject, String topicName,
                                        BigDecimal masteryPercent, ErrorType errorType) {
        WeakAreaRecord record = new WeakAreaRecord();
        record.id = UUID.randomUUID();
        record.studentId = studentId;
        record.enrollmentId = enrollmentId;
        record.subject = subject;
        record.topicName = topicName;
        record.masteryPercent = masteryPercent.setScale(2, RoundingMode.HALF_UP);
        record.primaryErrorType = errorType;
        record.incorrectAttempts = 0;
        record.totalAttempts = 0;
        record.prerequisitesWeak = false;
        record.detectedAt = Instant.now();
        record.createdAt = Instant.now();
        record.updatedAt = Instant.now();
        return record;
    }

    public void updateAttempts(Integer incorrectAttempts, Integer totalAttempts) {
        this.incorrectAttempts = incorrectAttempts;
        this.totalAttempts = totalAttempts;
        this.updatedAt = Instant.now();
    }

    public void markPrerequisitesWeak(boolean weak) {
        this.prerequisitesWeak = weak;
        this.updatedAt = Instant.now();
    }

    public void setChapterName(String chapterName) {
        this.chapterName = chapterName;
        this.updatedAt = Instant.now();
    }

    public void markReviewed() {
        this.lastReviewedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public String getSubject() { return subject; }
    public String getTopicName() { return topicName; }
    public String getChapterName() { return chapterName; }
    public BigDecimal getMasteryPercent() { return masteryPercent; }
    public ErrorType getPrimaryErrorType() { return primaryErrorType; }
    public Integer getIncorrectAttempts() { return incorrectAttempts; }
    public Integer getTotalAttempts() { return totalAttempts; }
    public Boolean getPrerequisitesWeak() { return prerequisitesWeak; }
    public Instant getDetectedAt() { return detectedAt; }
    public Instant getLastReviewedAt() { return lastReviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public Long getVersion() { return version; }
}
