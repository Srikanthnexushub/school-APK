package com.edutech.aimentor.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "study_plans", schema = "aimentor_schema")
public class StudyPlan {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "student_id", updatable = false, nullable = false)
    private UUID studentId;

    @Column(name = "enrollment_id", updatable = false)
    private UUID enrollmentId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "target_exam_date")
    private LocalDate targetExamDate;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Version
    private Long version;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "studyPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<StudyPlanItem> items = new ArrayList<>();

    // Required by JPA
    protected StudyPlan() {}

    private StudyPlan(UUID id, UUID studentId, UUID enrollmentId, String title,
                      String description, LocalDate targetExamDate) {
        this.id = id;
        this.studentId = studentId;
        this.enrollmentId = enrollmentId;
        this.title = title;
        this.description = description;
        this.targetExamDate = targetExamDate;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static StudyPlan create(UUID studentId, UUID enrollmentId, String title,
                                   String description, LocalDate targetExamDate) {
        return new StudyPlan(UUID.randomUUID(), studentId, enrollmentId, title, description, targetExamDate);
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public void addItem(StudyPlanItem item) {
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDate getTargetExamDate() { return targetExamDate; }
    public boolean isActive() { return active; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public List<StudyPlanItem> getItems() { return Collections.unmodifiableList(items); }
}
