// src/main/java/com/edutech/center/domain/model/Attendance.java
package com.edutech.center.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Attendance record — immutable once marked (no @Version, no UPDATE after creation).
 * A teacher may re-mark by deleting and re-creating (handled at service layer).
 * Unique constraint prevents duplicate entries per student per batch per date.
 */
@Entity
@Table(
    name = "attendance",
    schema = "center_schema",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_attendance_student_batch_date",
        columnNames = {"batch_id", "student_id", "date"}
    )
)
public class Attendance {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "batch_id", updatable = false, nullable = false)
    private UUID batchId;

    @Column(name = "center_id", updatable = false, nullable = false)
    private UUID centerId;

    @Column(name = "student_id", updatable = false, nullable = false)
    private UUID studentId;

    @Column(updatable = false, nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false, nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "marked_by_teacher_id", updatable = false, nullable = false)
    private UUID markedByTeacherId;

    @Column(updatable = false, length = 500)
    private String notes;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    protected Attendance() {}

    private Attendance(UUID id, UUID batchId, UUID centerId, UUID studentId,
                       LocalDate date, AttendanceStatus status,
                       UUID markedByTeacherId, String notes) {
        this.id = id;
        this.batchId = batchId;
        this.centerId = centerId;
        this.studentId = studentId;
        this.date = date;
        this.status = status;
        this.markedByTeacherId = markedByTeacherId;
        this.notes = notes;
        this.createdAt = Instant.now();
    }

    public static Attendance mark(UUID batchId, UUID centerId, UUID studentId,
                                  LocalDate date, AttendanceStatus status,
                                  UUID markedByTeacherId, String notes) {
        return new Attendance(UUID.randomUUID(), batchId, centerId, studentId,
                date, status, markedByTeacherId, notes);
    }

    public UUID getId() { return id; }
    public UUID getBatchId() { return batchId; }
    public UUID getCenterId() { return centerId; }
    public UUID getStudentId() { return studentId; }
    public LocalDate getDate() { return date; }
    public AttendanceStatus getStatus() { return status; }
    public UUID getMarkedByTeacherId() { return markedByTeacherId; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
}
