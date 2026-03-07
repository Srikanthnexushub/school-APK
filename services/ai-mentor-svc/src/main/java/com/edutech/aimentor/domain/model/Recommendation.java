package com.edutech.aimentor.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recommendations", schema = "aimentor_schema")
public class Recommendation {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "student_id", updatable = false, nullable = false)
    private UUID studentId;

    @Column(name = "enrollment_id", updatable = false, nullable = false)
    private UUID enrollmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_area", nullable = false, length = 50)
    private SubjectArea subjectArea;

    @Column(nullable = false, length = 255)
    private String topic;

    @Column(name = "recommendation_text", nullable = false, columnDefinition = "TEXT")
    private String recommendationText;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", nullable = false, length = 20)
    private PriorityLevel priorityLevel;

    @Column(name = "is_acknowledged", nullable = false)
    private boolean acknowledged;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    // Required by JPA
    protected Recommendation() {}

    private Recommendation(UUID id, UUID studentId, UUID enrollmentId, SubjectArea subjectArea,
                           String topic, String recommendationText, PriorityLevel priorityLevel,
                           Instant expiresAt) {
        this.id = id;
        this.studentId = studentId;
        this.enrollmentId = enrollmentId;
        this.subjectArea = subjectArea;
        this.topic = topic;
        this.recommendationText = recommendationText;
        this.priorityLevel = priorityLevel;
        this.acknowledged = false;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public static Recommendation create(UUID studentId, UUID enrollmentId, SubjectArea subjectArea,
                                        String topic, String recommendationText,
                                        PriorityLevel priorityLevel, Instant expiresAt) {
        return new Recommendation(UUID.randomUUID(), studentId, enrollmentId, subjectArea,
                                  topic, recommendationText, priorityLevel, expiresAt);
    }

    public void acknowledge() {
        this.acknowledged = true;
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public SubjectArea getSubjectArea() { return subjectArea; }
    public String getTopic() { return topic; }
    public String getRecommendationText() { return recommendationText; }
    public PriorityLevel getPriorityLevel() { return priorityLevel; }
    public boolean isAcknowledged() { return acknowledged; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
