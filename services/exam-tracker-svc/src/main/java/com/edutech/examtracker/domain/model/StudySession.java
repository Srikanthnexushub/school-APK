package com.edutech.examtracker.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Records a single study session for a student on a specific topic.
 * Accuracy is nullable for CONCEPT_LEARNING sessions.
 */
@Entity
@Table(name = "study_sessions", schema = "examtracker_schema")
public class StudySession {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "enrollment_id", nullable = false, updatable = false)
    private UUID enrollmentId;

    @Column(nullable = false, length = 100)
    private String subject;

    @Column(name = "topic_name", nullable = false, length = 200)
    private String topicName;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 30)
    private SessionType sessionType;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "questions_attempted")
    private Integer questionsAttempted;

    @Column(name = "accuracy_percent", precision = 5, scale = 2)
    private BigDecimal accuracyPercent;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    protected StudySession() {}

    private StudySession(UUID id, UUID studentId, UUID enrollmentId, String subject,
                         String topicName, SessionType sessionType, LocalDate sessionDate,
                         Integer durationMinutes) {
        this.id = id;
        this.studentId = studentId;
        this.enrollmentId = enrollmentId;
        this.subject = subject;
        this.topicName = topicName;
        this.sessionType = sessionType;
        this.sessionDate = sessionDate;
        this.durationMinutes = durationMinutes;
        this.createdAt = Instant.now();
    }

    public static StudySession create(UUID studentId, UUID enrollmentId, String subject,
                                      String topicName, SessionType sessionType,
                                      LocalDate sessionDate, Integer durationMinutes) {
        return new StudySession(UUID.randomUUID(), studentId, enrollmentId, subject,
                topicName, sessionType, sessionDate, durationMinutes);
    }

    public void setQuestionsAttempted(Integer questionsAttempted) {
        this.questionsAttempted = questionsAttempted;
    }

    public void setAccuracyPercent(BigDecimal accuracyPercent) {
        this.accuracyPercent = accuracyPercent;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public String getSubject() { return subject; }
    public String getTopicName() { return topicName; }
    public SessionType getSessionType() { return sessionType; }
    public LocalDate getSessionDate() { return sessionDate; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public Integer getQuestionsAttempted() { return questionsAttempted; }
    public BigDecimal getAccuracyPercent() { return accuracyPercent; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public Long getVersion() { return version; }
}
