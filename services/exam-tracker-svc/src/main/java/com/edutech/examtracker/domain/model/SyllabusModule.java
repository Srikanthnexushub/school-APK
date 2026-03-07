package com.edutech.examtracker.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single syllabus topic/module within an exam enrollment.
 * Tracks completion percentage and study status.
 */
@Entity
@Table(name = "syllabus_modules", schema = "examtracker_schema")
public class SyllabusModule {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "enrollment_id", nullable = false, updatable = false)
    private UUID enrollmentId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(nullable = false, length = 100)
    private String subject;

    @Column(name = "topic_name", nullable = false, length = 200)
    private String topicName;

    @Column(name = "chapter_name", nullable = false, length = 200)
    private String chapterName;

    @Column(name = "weightage_percent", nullable = false)
    private Integer weightagePercent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModuleStatus status;

    @Column(name = "completion_percent", nullable = false)
    private Integer completionPercent;

    @Column(name = "last_studied_at")
    private Instant lastStudiedAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    protected SyllabusModule() {}

    private SyllabusModule(UUID id, UUID enrollmentId, UUID studentId, String subject,
                           String topicName, String chapterName, Integer weightagePercent) {
        this.id = id;
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.subject = subject;
        this.topicName = topicName;
        this.chapterName = chapterName;
        this.weightagePercent = weightagePercent;
        this.status = ModuleStatus.NOT_STARTED;
        this.completionPercent = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static SyllabusModule create(UUID enrollmentId, UUID studentId, String subject,
                                        String topicName, String chapterName, Integer weightagePercent) {
        return new SyllabusModule(UUID.randomUUID(), enrollmentId, studentId, subject,
                topicName, chapterName, weightagePercent);
    }

    public void updateProgress(Integer completionPercent, ModuleStatus status) {
        this.completionPercent = completionPercent;
        this.status = status;
        this.lastStudiedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public UUID getStudentId() { return studentId; }
    public String getSubject() { return subject; }
    public String getTopicName() { return topicName; }
    public String getChapterName() { return chapterName; }
    public Integer getWeightagePercent() { return weightagePercent; }
    public ModuleStatus getStatus() { return status; }
    public Integer getCompletionPercent() { return completionPercent; }
    public Instant getLastStudiedAt() { return lastStudiedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public Long getVersion() { return version; }
}
