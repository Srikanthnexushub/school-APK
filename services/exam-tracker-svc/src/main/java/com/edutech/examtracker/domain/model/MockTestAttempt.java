package com.edutech.examtracker.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a single mock test attempt for a student targeting a specific exam.
 * Immutable after creation except for estimatedRank updates from official results.
 */
@Entity
@Table(name = "mock_test_attempts", schema = "examtracker_schema")
public class MockTestAttempt {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "enrollment_id", nullable = false, updatable = false)
    private UUID enrollmentId;

    @Column(name = "test_name", nullable = false, length = 300)
    private String testName;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_code", nullable = false, length = 30)
    private ExamCode examCode;

    @Column(name = "attempt_date", nullable = false)
    private LocalDate attemptDate;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(nullable = false)
    private Integer attempted;

    @Column(nullable = false)
    private Integer correct;

    @Column(nullable = false)
    private Integer incorrect;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal score;

    @Column(name = "max_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "accuracy_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal accuracyPercent;

    @Column(name = "time_taken_minutes", nullable = false)
    private Integer timeTakenMinutes;

    @Column(name = "total_time_minutes", nullable = false)
    private Integer totalTimeMinutes;

    @Column(name = "estimated_rank")
    private Integer estimatedRank;

    @Column(name = "subject_wise_json", columnDefinition = "TEXT")
    private String subjectWiseJson;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    protected MockTestAttempt() {}

    private MockTestAttempt(UUID id, UUID studentId, UUID enrollmentId, String testName,
                            ExamCode examCode, LocalDate attemptDate, Integer totalQuestions,
                            Integer attempted, Integer correct, Integer incorrect,
                            BigDecimal score, BigDecimal maxScore, BigDecimal accuracyPercent,
                            Integer timeTakenMinutes, Integer totalTimeMinutes,
                            String subjectWiseJson) {
        this.id = id;
        this.studentId = studentId;
        this.enrollmentId = enrollmentId;
        this.testName = testName;
        this.examCode = examCode;
        this.attemptDate = attemptDate;
        this.totalQuestions = totalQuestions;
        this.attempted = attempted;
        this.correct = correct;
        this.incorrect = incorrect;
        this.score = score;
        this.maxScore = maxScore;
        this.accuracyPercent = accuracyPercent;
        this.timeTakenMinutes = timeTakenMinutes;
        this.totalTimeMinutes = totalTimeMinutes;
        this.subjectWiseJson = subjectWiseJson;
        this.createdAt = Instant.now();
    }

    public static MockTestAttempt create(UUID studentId, UUID enrollmentId, String testName,
                                         ExamCode examCode, LocalDate attemptDate,
                                         Integer totalQuestions, Integer attempted,
                                         Integer correct, Integer incorrect,
                                         BigDecimal score, BigDecimal maxScore,
                                         BigDecimal accuracyPercent,
                                         Integer timeTakenMinutes, Integer totalTimeMinutes,
                                         String subjectWiseJson) {
        return new MockTestAttempt(UUID.randomUUID(), studentId, enrollmentId, testName,
                examCode, attemptDate, totalQuestions, attempted, correct, incorrect,
                score, maxScore, accuracyPercent, timeTakenMinutes, totalTimeMinutes,
                subjectWiseJson);
    }

    public void updateEstimatedRank(Integer estimatedRank) {
        this.estimatedRank = estimatedRank;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public String getTestName() { return testName; }
    public ExamCode getExamCode() { return examCode; }
    public LocalDate getAttemptDate() { return attemptDate; }
    public Integer getTotalQuestions() { return totalQuestions; }
    public Integer getAttempted() { return attempted; }
    public Integer getCorrect() { return correct; }
    public Integer getIncorrect() { return incorrect; }
    public BigDecimal getScore() { return score; }
    public BigDecimal getMaxScore() { return maxScore; }
    public BigDecimal getAccuracyPercent() { return accuracyPercent; }
    public Integer getTimeTakenMinutes() { return timeTakenMinutes; }
    public Integer getTotalTimeMinutes() { return totalTimeMinutes; }
    public Integer getEstimatedRank() { return estimatedRank; }
    public String getSubjectWiseJson() { return subjectWiseJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public Long getVersion() { return version; }
}
