package com.edutech.psych.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "session_histories", schema = "psych_schema")
public class SessionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "profile_id", updatable = false, nullable = false)
    private UUID profileId;

    @Column(name = "student_id", updatable = false, nullable = false)
    private UUID studentId;

    @Column(name = "center_id", updatable = false, nullable = false)
    private UUID centerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", updatable = false, nullable = false)
    private SessionType sessionType;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Version
    private Long version;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private SessionHistory() {
    }

    public static SessionHistory create(UUID profileId, UUID studentId, UUID centerId,
                                        SessionType sessionType, Instant scheduledAt) {
        SessionHistory session = new SessionHistory();
        session.profileId = profileId;
        session.studentId = studentId;
        session.centerId = centerId;
        session.sessionType = sessionType;
        session.scheduledAt = scheduledAt;
        session.status = SessionStatus.SCHEDULED;
        session.createdAt = Instant.now();
        session.updatedAt = Instant.now();
        return session;
    }

    public void start() {
        if (this.status != SessionStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "Session can only be started from SCHEDULED status. Current status: " + this.status);
        }
        this.status = SessionStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void complete(String notes) {
        if (this.status != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Session can only be completed from IN_PROGRESS status. Current status: " + this.status);
        }
        this.status = SessionStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.notes = notes;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (this.status != SessionStatus.SCHEDULED && this.status != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Session can only be cancelled from SCHEDULED or IN_PROGRESS status. Current status: " + this.status);
        }
        this.status = SessionStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProfileId() {
        return profileId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getCenterId() {
        return centerId;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getNotes() {
        return notes;
    }

    public SessionStatus getStatus() {
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

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
