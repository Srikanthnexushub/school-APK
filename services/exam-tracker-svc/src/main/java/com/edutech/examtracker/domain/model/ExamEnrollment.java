package com.edutech.examtracker.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Aggregate root representing a student's enrollment in a competitive exam.
 * State changes only via named domain methods. No public setters.
 */
@Entity
@Table(name = "exam_enrollments", schema = "examtracker_schema")
public class ExamEnrollment {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_code", nullable = false, length = 30)
    private ExamCode examCode;

    @Column(name = "exam_name", nullable = false, length = 200)
    private String examName;

    @Column(name = "exam_date")
    private LocalDate examDate;

    @Column(name = "target_year", nullable = false)
    private Integer targetYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExamStatus status;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Optional reference to an assess-svc exam UUID. Populated only for enrollments
     * that were auto-created by the Kafka consumer when a student submits an
     * institutional exam in assess-svc. Null for all manually-created competitive
     * exam enrollments.
     */
    @Column(name = "assess_exam_id")
    private UUID assessExamId;

    @Version
    private Long version;

    protected ExamEnrollment() {}

    private ExamEnrollment(UUID id, UUID studentId, ExamCode examCode, String examName,
                           Integer targetYear, LocalDate examDate, UUID assessExamId) {
        this.id = id;
        this.studentId = studentId;
        this.examCode = examCode;
        this.examName = examName;
        this.targetYear = targetYear;
        this.examDate = examDate;
        this.assessExamId = assessExamId;
        this.status = ExamStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static ExamEnrollment create(UUID studentId, ExamCode examCode, String examName,
                                        Integer targetYear) {
        return new ExamEnrollment(UUID.randomUUID(), studentId, examCode, examName, targetYear, null, null);
    }

    public static ExamEnrollment create(UUID studentId, ExamCode examCode, String examName,
                                        Integer targetYear, LocalDate examDate) {
        return new ExamEnrollment(UUID.randomUUID(), studentId, examCode, examName, targetYear, examDate, null);
    }

    /**
     * Factory for auto-synced enrollments created via assess-svc submission events.
     * The enrollment is created directly in COMPLETED status since the exam is already done.
     */
    public static ExamEnrollment createFromAssessEvent(UUID studentId, UUID assessExamId,
                                                        String examName, int targetYear) {
        ExamEnrollment e = new ExamEnrollment(
                UUID.randomUUID(), studentId, ExamCode.SCHOOL_EXAM, examName, targetYear, null, assessExamId
        );
        e.status = ExamStatus.COMPLETED;
        return e;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        this.status = ExamStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void drop() {
        this.status = ExamStatus.DROPPED;
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public ExamCode getExamCode() { return examCode; }
    public String getExamName() { return examName; }
    public LocalDate getExamDate() { return examDate; }
    public Integer getTargetYear() { return targetYear; }
    public ExamStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public UUID getAssessExamId() { return assessExamId; }
    public Long getVersion() { return version; }
}
