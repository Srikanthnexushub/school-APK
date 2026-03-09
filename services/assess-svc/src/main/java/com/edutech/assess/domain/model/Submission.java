// src/main/java/com/edutech/assess/domain/model/Submission.java
package com.edutech.assess.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "submissions", schema = "assess_schema")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "exam_id", nullable = false, updatable = false)
    private UUID examId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "enrollment_id", nullable = false, updatable = false)
    private UUID enrollmentId;

    @Column(name = "attempt_number", nullable = false, updatable = false)
    private int attemptNumber;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "total_marks")
    private double totalMarks;

    @Column(name = "scored_marks")
    private double scoredMarks;

    @Column
    private double percentage;

    @Column(name = "theta_estimate")
    private Double thetaEstimate; // IRT 3PL theta after final submission (null until graded)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;

    @Version
    private Long version;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    private Submission() {}

    public static Submission create(UUID examId, UUID studentId, UUID enrollmentId, int attemptNumber) {
        Submission sub = new Submission();
        sub.examId = examId;
        sub.studentId = studentId;
        sub.enrollmentId = enrollmentId;
        sub.attemptNumber = attemptNumber;
        sub.status = SubmissionStatus.IN_PROGRESS;
        sub.startedAt = Instant.now();
        sub.createdAt = Instant.now();
        sub.updatedAt = Instant.now();
        return sub;
    }

    public void grade(double scoredMarks, double totalMarks) {
        if (this.status != SubmissionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Submission is not in progress");
        }
        this.scoredMarks = scoredMarks;
        this.totalMarks = totalMarks;
        this.percentage = totalMarks > 0 ? (scoredMarks / totalMarks) * 100.0 : 0.0;
        this.status = SubmissionStatus.GRADED;
        this.submittedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void recordThetaEstimate(double theta) {
        this.thetaEstimate = theta;
        this.updatedAt = Instant.now();
    }

    public void invalidate() {
        this.status = SubmissionStatus.INVALIDATED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getExamId() { return examId; }
    public UUID getStudentId() { return studentId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public int getAttemptNumber() { return attemptNumber; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public double getTotalMarks() { return totalMarks; }
    public double getScoredMarks() { return scoredMarks; }
    public double getPercentage() { return percentage; }
    public Double getThetaEstimate() { return thetaEstimate; }
    public SubmissionStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
