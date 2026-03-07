// src/main/java/com/edutech/center/domain/model/FeeStructure.java
package com.edutech.center.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fee_structures", schema = "center_schema")
public class FeeStructure {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "center_id", updatable = false, nullable = false)
    private UUID centerId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 5)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeeFrequency frequency;

    @Column(name = "due_day", nullable = false)
    private int dueDay;

    @Column(name = "late_fee_amount", precision = 12, scale = 2)
    private BigDecimal lateFeeAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeeStatus status;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected FeeStructure() {}

    private FeeStructure(UUID id, UUID centerId, String name, String description,
                         BigDecimal amount, String currency, FeeFrequency frequency,
                         int dueDay, BigDecimal lateFeeAmount) {
        this.id = id;
        this.centerId = centerId;
        this.name = name;
        this.description = description;
        this.amount = amount;
        this.currency = currency;
        this.frequency = frequency;
        this.dueDay = dueDay;
        this.lateFeeAmount = lateFeeAmount;
        this.status = FeeStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static FeeStructure create(UUID centerId, String name, String description,
                                      BigDecimal amount, String currency,
                                      FeeFrequency frequency, int dueDay,
                                      BigDecimal lateFeeAmount) {
        return new FeeStructure(UUID.randomUUID(), centerId, name, description,
                amount, currency, frequency, dueDay, lateFeeAmount);
    }

    public void archive() {
        this.status = FeeStatus.ARCHIVED;
        this.updatedAt = Instant.now();
    }

    public enum FeeStatus { ACTIVE, ARCHIVED }

    public UUID getId() { return id; }
    public UUID getCenterId() { return centerId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public FeeFrequency getFrequency() { return frequency; }
    public int getDueDay() { return dueDay; }
    public BigDecimal getLateFeeAmount() { return lateFeeAmount; }
    public FeeStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
