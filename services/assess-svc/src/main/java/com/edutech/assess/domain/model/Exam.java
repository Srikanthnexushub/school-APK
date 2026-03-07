// src/main/java/com/edutech/assess/domain/model/Exam.java
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
@Table(name = "exams", schema = "assess_schema")
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "batch_id", nullable = false, updatable = false)
    private UUID batchId;

    @Column(name = "center_id", nullable = false, updatable = false)
    private UUID centerId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamMode mode;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Column(name = "total_marks", nullable = false)
    private double totalMarks;

    @Column(name = "passing_marks", nullable = false)
    private double passingMarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamStatus status;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private Exam() {}

    public static Exam create(UUID batchId, UUID centerId, String title, String description,
                               ExamMode mode, int durationMinutes, int maxAttempts,
                               Instant startAt, Instant endAt, double totalMarks, double passingMarks) {
        Exam exam = new Exam();
        exam.id = UUID.randomUUID();
        exam.batchId = batchId;
        exam.centerId = centerId;
        exam.title = title;
        exam.description = description;
        exam.mode = mode;
        exam.durationMinutes = durationMinutes;
        exam.maxAttempts = maxAttempts;
        exam.startAt = startAt;
        exam.endAt = endAt;
        exam.totalMarks = totalMarks;
        exam.passingMarks = passingMarks;
        exam.status = ExamStatus.DRAFT;
        exam.createdAt = Instant.now();
        exam.updatedAt = Instant.now();
        return exam;
    }

    public void publish() {
        if (this.status != ExamStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT exams can be published");
        }
        this.status = ExamStatus.PUBLISHED;
        this.updatedAt = Instant.now();
    }

    public void close() {
        if (this.status != ExamStatus.PUBLISHED) {
            throw new IllegalStateException("Only PUBLISHED exams can be closed");
        }
        this.status = ExamStatus.CLOSED;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (this.status == ExamStatus.CLOSED) {
            throw new IllegalStateException("CLOSED exams cannot be cancelled");
        }
        this.status = ExamStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getBatchId() { return batchId; }
    public UUID getCenterId() { return centerId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public ExamMode getMode() { return mode; }
    public int getDurationMinutes() { return durationMinutes; }
    public int getMaxAttempts() { return maxAttempts; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public double getTotalMarks() { return totalMarks; }
    public double getPassingMarks() { return passingMarks; }
    public ExamStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
