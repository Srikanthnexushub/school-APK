// src/main/java/com/edutech/center/domain/model/Batch.java
package com.edutech.center.domain.model;

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
 * A batch is a cohort of students studying a subject together within a center.
 * Teacher assignment and enrollment count are managed here.
 */
@Entity
@Table(name = "batches", schema = "center_schema")
public class Batch {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "center_id", updatable = false, nullable = false)
    private UUID centerId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String subject;

    @Column(name = "teacher_id")
    private UUID teacherId;

    @Column(name = "max_students", nullable = false)
    private int maxStudents;

    @Column(name = "enrolled_count", nullable = false)
    private int enrolledCount;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BatchStatus status;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    protected Batch() {}

    private Batch(UUID id, UUID centerId, String name, String code, String subject,
                  UUID teacherId, int maxStudents, LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.centerId = centerId;
        this.name = name;
        this.code = code;
        this.subject = subject;
        this.teacherId = teacherId;
        this.maxStudents = maxStudents;
        this.enrolledCount = 0;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = BatchStatus.UPCOMING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static Batch create(UUID centerId, String name, String code, String subject,
                               UUID teacherId, int maxStudents,
                               LocalDate startDate, LocalDate endDate) {
        return new Batch(UUID.randomUUID(), centerId, name, code, subject,
                teacherId, maxStudents, startDate, endDate);
    }

    public void assignTeacher(UUID teacherId) {
        this.teacherId = teacherId;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (this.status != BatchStatus.UPCOMING) {
            throw new IllegalStateException("Only UPCOMING batches can be activated");
        }
        this.status = BatchStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (this.status != BatchStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE batches can be completed");
        }
        this.status = BatchStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (this.status == BatchStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed batch");
        }
        this.status = BatchStatus.CANCELLED;
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void incrementEnrollment() {
        if (this.enrolledCount >= this.maxStudents) {
            throw new IllegalStateException("Batch is at maximum capacity");
        }
        this.enrolledCount++;
        this.updatedAt = Instant.now();
    }

    public void decrementEnrollment() {
        if (this.enrolledCount > 0) {
            this.enrolledCount--;
            this.updatedAt = Instant.now();
        }
    }

    public boolean hasCapacity() { return this.enrolledCount < this.maxStudents; }
    public boolean isActive() { return this.status == BatchStatus.ACTIVE && this.deletedAt == null; }

    public UUID getId() { return id; }
    public UUID getCenterId() { return centerId; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getSubject() { return subject; }
    public UUID getTeacherId() { return teacherId; }
    public int getMaxStudents() { return maxStudents; }
    public int getEnrolledCount() { return enrolledCount; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public BatchStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public Long getVersion() { return version; }
}
