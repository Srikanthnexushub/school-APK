package com.edutech.aimentor.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "doubt_tickets", schema = "aimentor_schema")
public class DoubtTicket {

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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "ai_answer", columnDefinition = "TEXT")
    private String aiAnswer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DoubtStatus status;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Version
    private Long version;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Required by JPA
    protected DoubtTicket() {}

    private DoubtTicket(UUID id, UUID studentId, UUID enrollmentId,
                        SubjectArea subjectArea, String question) {
        this.id = id;
        this.studentId = studentId;
        this.enrollmentId = enrollmentId;
        this.subjectArea = subjectArea;
        this.question = question;
        this.status = DoubtStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public static DoubtTicket create(UUID studentId, UUID enrollmentId,
                                     SubjectArea subjectArea, String question) {
        return new DoubtTicket(UUID.randomUUID(), studentId, enrollmentId, subjectArea, question);
    }

    public void resolve(String aiAnswer) {
        this.aiAnswer = aiAnswer;
        this.status = DoubtStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }

    public void escalate() {
        this.status = DoubtStatus.ESCALATED;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public SubjectArea getSubjectArea() { return subjectArea; }
    public String getQuestion() { return question; }
    public String getAiAnswer() { return aiAnswer; }
    public DoubtStatus getStatus() { return status; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
