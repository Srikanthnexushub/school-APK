package com.edutech.student.domain.model;

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
@Table(name = "target_exams", schema = "student_schema")
public class TargetExam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_code", nullable = false)
    private ExamCode examCode;

    @Column(name = "target_year", nullable = false)
    private Integer targetYear;

    @Column(nullable = false)
    private Integer priority;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private TargetExam() {}

    public static TargetExam create(UUID studentId, ExamCode examCode, Integer targetYear, Integer priority) {
        TargetExam exam = new TargetExam();
        exam.studentId = studentId;
        exam.examCode = examCode;
        exam.targetYear = targetYear;
        exam.priority = priority;
        exam.createdAt = Instant.now();
        return exam;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public ExamCode getExamCode() {
        return examCode;
    }

    public Integer getTargetYear() {
        return targetYear;
    }

    public Integer getPriority() {
        return priority;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
