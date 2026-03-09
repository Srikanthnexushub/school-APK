// src/main/java/com/edutech/assess/domain/model/Grade.java
package com.edutech.assess.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "grades", schema = "assess_schema")
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "submission_id", nullable = false, updatable = false, unique = true)
    private UUID submissionId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "exam_id", nullable = false, updatable = false)
    private UUID examId;

    @Column(name = "batch_id", nullable = false, updatable = false)
    private UUID batchId;

    @Column(name = "center_id", nullable = false, updatable = false)
    private UUID centerId;

    @Column(nullable = false)
    private double percentage;

    @Column(name = "letter_grade", nullable = false)
    private String letterGrade;

    @Column(nullable = false)
    private boolean passed;

    @Version
    private Long version;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    private Grade() {}

    public static Grade create(UUID submissionId, UUID studentId, UUID examId,
                                UUID batchId, UUID centerId,
                                double percentage, double passingPercentage) {
        Grade grade = new Grade();
        grade.submissionId = submissionId;
        grade.studentId = studentId;
        grade.examId = examId;
        grade.batchId = batchId;
        grade.centerId = centerId;
        grade.percentage = percentage;
        grade.letterGrade = computeLetterGrade(percentage);
        grade.passed = percentage >= passingPercentage;
        grade.createdAt = Instant.now();
        grade.updatedAt = Instant.now();
        return grade;
    }

    private static String computeLetterGrade(double pct) {
        if (pct >= 90) return "A";
        if (pct >= 80) return "B";
        if (pct >= 70) return "C";
        if (pct >= 60) return "D";
        return "F";
    }

    public UUID getId() { return id; }
    public UUID getSubmissionId() { return submissionId; }
    public UUID getStudentId() { return studentId; }
    public UUID getExamId() { return examId; }
    public UUID getBatchId() { return batchId; }
    public UUID getCenterId() { return centerId; }
    public double getPercentage() { return percentage; }
    public String getLetterGrade() { return letterGrade; }
    public boolean isPassed() { return passed; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
