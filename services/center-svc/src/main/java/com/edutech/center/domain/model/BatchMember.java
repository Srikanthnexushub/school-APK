// src/main/java/com/edutech/center/domain/model/BatchMember.java
package com.edutech.center.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Mirrors a student's membership in a batch.
 * Populated by consuming StudentBatchEnrollmentEvent from Kafka (student-profile-svc).
 * Used by attendance module to provide teacher with roster.
 */
@Entity
@Table(
    name = "batch_members",
    schema = "center_schema",
    uniqueConstraints = @UniqueConstraint(name = "uq_batch_member", columnNames = {"batch_id", "student_id"})
)
public class BatchMember {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "batch_id", nullable = false, updatable = false)
    private UUID batchId;

    @Column(name = "center_id", nullable = false, updatable = false)
    private UUID centerId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "student_name", length = 200)
    private String studentName;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    protected BatchMember() {}

    private BatchMember(UUID id, UUID batchId, UUID centerId, UUID studentId,
                        String studentName, Instant enrolledAt) {
        this.id = id;
        this.batchId = batchId;
        this.centerId = centerId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.enrolledAt = enrolledAt;
    }

    public static BatchMember enroll(UUID batchId, UUID centerId, UUID studentId, String studentName) {
        return new BatchMember(UUID.randomUUID(), batchId, centerId, studentId, studentName, Instant.now());
    }

    public void withdraw() {
        this.withdrawnAt = Instant.now();
    }

    public boolean isActive() { return withdrawnAt == null; }

    public UUID getId()          { return id; }
    public UUID getBatchId()     { return batchId; }
    public UUID getCenterId()    { return centerId; }
    public UUID getStudentId()   { return studentId; }
    public String getStudentName() { return studentName; }
    public Instant getEnrolledAt() { return enrolledAt; }
    public Instant getWithdrawnAt() { return withdrawnAt; }
}
