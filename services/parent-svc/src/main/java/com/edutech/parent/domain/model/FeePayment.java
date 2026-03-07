// src/main/java/com/edutech/parent/domain/model/FeePayment.java
package com.edutech.parent.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fee_payments", schema = "parent_schema")
public class FeePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "center_id", nullable = false)
    private UUID centerId;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "amount_paid", precision = 12, scale = 2, nullable = false)
    private BigDecimal amountPaid;

    @Column(nullable = false)
    private String currency;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "reference_number", nullable = false)
    private String referenceNumber;

    @Column
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private FeePayment() {}

    public static FeePayment create(UUID parentId, UUID studentId, UUID centerId, UUID batchId,
                                     BigDecimal amountPaid, String currency, LocalDate paymentDate,
                                     String referenceNumber, String remarks) {
        FeePayment payment = new FeePayment();
        payment.parentId = parentId;
        payment.studentId = studentId;
        payment.centerId = centerId;
        payment.batchId = batchId;
        payment.amountPaid = amountPaid;
        payment.currency = currency;
        payment.paymentDate = paymentDate;
        payment.referenceNumber = referenceNumber;
        payment.remarks = remarks;
        payment.status = PaymentStatus.PENDING;
        payment.createdAt = Instant.now();
        payment.updatedAt = payment.createdAt;
        return payment;
    }

    public void confirm() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Cannot confirm a payment that is not PENDING. Current status: " + this.status);
        }
        this.status = PaymentStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }

    public void dispute() {
        if (this.status != PaymentStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot dispute a payment that is not CONFIRMED. Current status: " + this.status);
        }
        this.status = PaymentStatus.DISPUTED;
        this.updatedAt = Instant.now();
    }

    public void refund() {
        if (this.status != PaymentStatus.DISPUTED) {
            throw new IllegalStateException("Cannot refund a payment that is not DISPUTED. Current status: " + this.status);
        }
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getParentId() {
        return parentId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getCenterId() {
        return centerId;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public String getRemarks() {
        return remarks;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
