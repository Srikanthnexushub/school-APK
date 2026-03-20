// src/main/java/com/edutech/assess/domain/model/Assignment.java
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
@Table(name = "assignments", schema = "assess_schema")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "batch_id", nullable = false, updatable = false)
    private UUID batchId;

    @Column(name = "center_id", nullable = false, updatable = false)
    private UUID centerId;

    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AssignmentType type;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "total_marks", nullable = false)
    private double totalMarks;

    @Column(name = "passing_marks", nullable = false)
    private double passingMarks;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "attachment_url")
    private String attachmentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private Assignment() {}

    public static Assignment create(UUID batchId, UUID centerId, UUID createdByUserId,
                                    String title, String description, AssignmentType type,
                                    Instant dueDate, double totalMarks, double passingMarks,
                                    String instructions, String attachmentUrl) {
        Assignment a = new Assignment();
        a.batchId = batchId;
        a.centerId = centerId;
        a.createdByUserId = createdByUserId;
        a.title = title;
        a.description = description;
        a.type = type;
        a.dueDate = dueDate;
        a.totalMarks = totalMarks;
        a.passingMarks = passingMarks;
        a.instructions = instructions;
        a.attachmentUrl = attachmentUrl;
        a.status = AssignmentStatus.DRAFT;
        a.createdAt = Instant.now();
        a.updatedAt = Instant.now();
        return a;
    }

    public void publish() {
        if (this.status != AssignmentStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT assignments can be published");
        }
        this.status = AssignmentStatus.PUBLISHED;
        this.updatedAt = Instant.now();
    }

    public void close() {
        if (this.status != AssignmentStatus.PUBLISHED) {
            throw new IllegalStateException("Only PUBLISHED assignments can be closed");
        }
        this.status = AssignmentStatus.CLOSED;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (this.status == AssignmentStatus.CLOSED) {
            throw new IllegalStateException("CLOSED assignments cannot be cancelled");
        }
        this.status = AssignmentStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void updateDetails(String title, String description, String instructions,
                               String attachmentUrl, Instant dueDate,
                               double totalMarks, double passingMarks) {
        if (this.status != AssignmentStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT assignments can be updated");
        }
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (instructions != null) this.instructions = instructions;
        if (attachmentUrl != null) this.attachmentUrl = attachmentUrl;
        if (dueDate != null) this.dueDate = dueDate;
        if (totalMarks > 0) this.totalMarks = totalMarks;
        if (passingMarks >= 0) this.passingMarks = passingMarks;
        this.updatedAt = Instant.now();
    }

    public boolean isOwnedBy(UUID userId) {
        return this.createdByUserId.equals(userId);
    }

    public boolean belongsToCenter(UUID cId) {
        return this.centerId.equals(cId);
    }

    public boolean belongsToBatch(UUID bId) {
        return this.batchId.equals(bId);
    }

    public UUID getId() { return id; }
    public UUID getBatchId() { return batchId; }
    public UUID getCenterId() { return centerId; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public AssignmentType getType() { return type; }
    public Instant getDueDate() { return dueDate; }
    public double getTotalMarks() { return totalMarks; }
    public double getPassingMarks() { return passingMarks; }
    public String getInstructions() { return instructions; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public AssignmentStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
