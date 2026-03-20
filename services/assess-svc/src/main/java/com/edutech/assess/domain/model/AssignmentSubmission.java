// src/main/java/com/edutech/assess/domain/model/AssignmentSubmission.java
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
@Table(name = "assignment_submissions", schema = "assess_schema")
public class AssignmentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "assignment_id", nullable = false, updatable = false)
    private UUID assignmentId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "text_response", columnDefinition = "TEXT")
    private String textResponse;

    @Column(name = "score")
    private Double score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentSubmissionStatus status;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "graded_at")
    private Instant gradedAt;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private AssignmentSubmission() {}

    public static AssignmentSubmission create(UUID assignmentId, UUID studentId) {
        AssignmentSubmission s = new AssignmentSubmission();
        s.assignmentId = assignmentId;
        s.studentId = studentId;
        s.status = AssignmentSubmissionStatus.PENDING;
        s.createdAt = Instant.now();
        s.updatedAt = Instant.now();
        return s;
    }

    public void submit(String textResponse) {
        if (this.status != AssignmentSubmissionStatus.PENDING
                && this.status != AssignmentSubmissionStatus.SUBMITTED) {
            throw new IllegalStateException("Cannot submit from status: " + this.status);
        }
        this.textResponse = textResponse;
        this.status = AssignmentSubmissionStatus.SUBMITTED;
        this.submittedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void submitLate(String textResponse) {
        this.textResponse = textResponse;
        this.status = AssignmentSubmissionStatus.LATE;
        this.submittedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void grade(double score, String feedback) {
        if (this.status != AssignmentSubmissionStatus.SUBMITTED
                && this.status != AssignmentSubmissionStatus.LATE) {
            throw new IllegalStateException("Cannot grade from status: " + this.status);
        }
        this.score = score;
        this.feedback = feedback;
        this.status = AssignmentSubmissionStatus.GRADED;
        this.gradedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isOwnedBy(UUID studentId) {
        return this.studentId.equals(studentId);
    }

    public UUID getId() { return id; }
    public UUID getAssignmentId() { return assignmentId; }
    public UUID getStudentId() { return studentId; }
    public String getTextResponse() { return textResponse; }
    public Double getScore() { return score; }
    public String getFeedback() { return feedback; }
    public AssignmentSubmissionStatus getStatus() { return status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getGradedAt() { return gradedAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
