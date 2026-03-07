package com.edutech.aimentor.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Study plan item with SM-2 spaced repetition algorithm fields.
 *
 * SM-2 fields:
 *   interval     — number of days until the next review
 *   repetitions  — number of consecutive correct reviews
 *   easeFactor   — difficulty multiplier (default 2.5, min 1.3)
 *   nextReviewAt — scheduled next review date
 *   lastReviewedAt — date of last review
 *   quality      — recall quality from last review (0-5)
 */
@Entity
@Table(name = "study_plan_items", schema = "aimentor_schema")
public class StudyPlanItem {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_plan_id", nullable = false, updatable = false)
    private StudyPlan studyPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_area", nullable = false, length = 50)
    private SubjectArea subjectArea;

    @Column(nullable = false, length = 255)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", nullable = false, length = 20)
    private PriorityLevel priorityLevel;

    // SM-2 fields
    @Column(name = "interval_days", nullable = false)
    private int interval;

    @Column(nullable = false)
    private int repetitions;

    @Column(name = "ease_factor", nullable = false, precision = 4, scale = 2)
    private BigDecimal easeFactor;

    @Column(name = "next_review_at")
    private LocalDate nextReviewAt;

    @Column(name = "last_reviewed_at")
    private LocalDate lastReviewedAt;

    @Column
    private Integer quality;

    @Version
    private Long version;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Required by JPA
    protected StudyPlanItem() {}

    private StudyPlanItem(UUID id, StudyPlan studyPlan, SubjectArea subjectArea,
                          String topic, PriorityLevel priorityLevel) {
        this.id = id;
        this.studyPlan = studyPlan;
        this.subjectArea = subjectArea;
        this.topic = topic;
        this.priorityLevel = priorityLevel;
        this.interval = 1;
        this.repetitions = 0;
        this.easeFactor = new BigDecimal("2.50");
        this.nextReviewAt = LocalDate.now().plusDays(1);
        this.createdAt = Instant.now();
    }

    public static StudyPlanItem create(StudyPlan studyPlan, SubjectArea subjectArea,
                                       String topic, PriorityLevel priorityLevel) {
        return new StudyPlanItem(UUID.randomUUID(), studyPlan, subjectArea, topic, priorityLevel);
    }

    /**
     * Apply SM-2 algorithm update after a review.
     *
     * @param reviewQuality quality of recall (0-5), where >=3 is considered correct
     */
    public void applyReview(int reviewQuality) {
        if (reviewQuality < 0 || reviewQuality > 5) {
            throw new IllegalArgumentException("Quality must be between 0 and 5, got: " + reviewQuality);
        }

        this.quality = reviewQuality;
        this.lastReviewedAt = LocalDate.now();

        if (reviewQuality >= 3) {
            // Correct response — advance interval
            if (this.repetitions == 0) {
                this.interval = 1;
            } else if (this.repetitions == 1) {
                this.interval = 6;
            } else {
                this.interval = (int) Math.round(this.interval * this.easeFactor.doubleValue());
            }
            this.repetitions++;
        } else {
            // Incorrect response — reset
            this.repetitions = 0;
            this.interval = 1;
        }

        // Update ease factor: EF' = EF + (0.1 - (5-q)*(0.08+(5-q)*0.02))
        double newEaseFactor = this.easeFactor.doubleValue()
            + (0.1 - (5 - reviewQuality) * (0.08 + (5 - reviewQuality) * 0.02));
        // Minimum ease factor is 1.3
        if (newEaseFactor < 1.3) {
            newEaseFactor = 1.3;
        }
        this.easeFactor = BigDecimal.valueOf(Math.round(newEaseFactor * 100.0) / 100.0);
        this.nextReviewAt = LocalDate.now().plusDays(this.interval);
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public StudyPlan getStudyPlan() { return studyPlan; }
    public SubjectArea getSubjectArea() { return subjectArea; }
    public String getTopic() { return topic; }
    public PriorityLevel getPriorityLevel() { return priorityLevel; }
    public int getInterval() { return interval; }
    public int getRepetitions() { return repetitions; }
    public BigDecimal getEaseFactor() { return easeFactor; }
    public LocalDate getNextReviewAt() { return nextReviewAt; }
    public LocalDate getLastReviewedAt() { return lastReviewedAt; }
    public Integer getQuality() { return quality; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
