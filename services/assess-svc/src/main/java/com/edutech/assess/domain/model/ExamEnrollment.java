// src/main/java/com/edutech/assess/domain/model/ExamEnrollment.java
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
@Table(name = "exam_enrollments", schema = "assess_schema")
public class ExamEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "exam_id", nullable = false, updatable = false)
    private UUID examId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status;

    @Version
    private Long version;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private ExamEnrollment() {}

    public static ExamEnrollment create(UUID examId, UUID studentId) {
        ExamEnrollment enrollment = new ExamEnrollment();
        enrollment.id = UUID.randomUUID();
        enrollment.examId = examId;
        enrollment.studentId = studentId;
        enrollment.status = EnrollmentStatus.ENROLLED;
        enrollment.enrolledAt = Instant.now();
        enrollment.updatedAt = Instant.now();
        return enrollment;
    }

    public void withdraw() {
        if (this.status == EnrollmentStatus.WITHDRAWN) {
            throw new IllegalStateException("Enrollment is already withdrawn");
        }
        this.status = EnrollmentStatus.WITHDRAWN;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getExamId() { return examId; }
    public UUID getStudentId() { return studentId; }
    public EnrollmentStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getEnrolledAt() { return enrolledAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
