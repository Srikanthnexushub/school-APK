// src/main/java/com/edutech/center/domain/model/BatchFeeAssignment.java
package com.edutech.center.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Links a fee structure to a batch for a date range.
 * Allows different batches in the same center to have different fee structures.
 */
@Entity
@Table(
    name = "batch_fee_assignments",
    schema = "center_schema",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_batch_fee_active",
        columnNames = {"batch_id", "fee_structure_id", "effective_from"}
    )
)
public class BatchFeeAssignment {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "batch_id", nullable = false, updatable = false)
    private UUID batchId;

    @Column(name = "center_id", nullable = false, updatable = false)
    private UUID centerId;

    @Column(name = "fee_structure_id", nullable = false, updatable = false)
    private UUID feeStructureId;

    @Column(name = "effective_from", nullable = false, updatable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BatchFeeAssignment() {}

    private BatchFeeAssignment(UUID id, UUID batchId, UUID centerId, UUID feeStructureId,
                               LocalDate effectiveFrom, LocalDate effectiveTo) {
        this.id = id;
        this.batchId = batchId;
        this.centerId = centerId;
        this.feeStructureId = feeStructureId;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdAt = Instant.now();
    }

    public static BatchFeeAssignment assign(UUID batchId, UUID centerId, UUID feeStructureId,
                                            LocalDate effectiveFrom, LocalDate effectiveTo) {
        return new BatchFeeAssignment(UUID.randomUUID(), batchId, centerId, feeStructureId,
                effectiveFrom, effectiveTo);
    }

    public UUID getId()             { return id; }
    public UUID getBatchId()        { return batchId; }
    public UUID getCenterId()       { return centerId; }
    public UUID getFeeStructureId() { return feeStructureId; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public LocalDate getEffectiveTo()   { return effectiveTo; }
    public Instant getCreatedAt()   { return createdAt; }
}
