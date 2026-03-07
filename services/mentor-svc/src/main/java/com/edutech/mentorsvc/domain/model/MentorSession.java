package com.edutech.mentorsvc.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "mentor_sessions", schema = "mentor_schema")
public class MentorSession {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mentor_id", nullable = false)
    private MentorProfile mentor;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "scheduled_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime scheduledAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_mode", nullable = false, length = 20)
    private SessionMode sessionMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @Column(name = "completed_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime completedAt;

    @Column(name = "deleted_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime deletedAt;

    protected MentorSession() {
    }

    private MentorSession(UUID id, MentorProfile mentor, UUID studentId, OffsetDateTime scheduledAt,
                          int durationMinutes, SessionMode sessionMode, String meetingLink, String notes) {
        this.id = id;
        this.mentor = mentor;
        this.studentId = studentId;
        this.scheduledAt = scheduledAt;
        this.durationMinutes = durationMinutes;
        this.sessionMode = sessionMode;
        this.status = SessionStatus.SCHEDULED;
        this.meetingLink = meetingLink;
        this.notes = notes;
        this.createdAt = OffsetDateTime.now();
    }

    public static MentorSession create(MentorProfile mentor, UUID studentId, OffsetDateTime scheduledAt,
                                       int durationMinutes, SessionMode sessionMode,
                                       String meetingLink, String notes) {
        return new MentorSession(UUID.randomUUID(), mentor, studentId, scheduledAt,
                durationMinutes, sessionMode, meetingLink, notes);
    }

    public void markInProgress() {
        this.status = SessionStatus.IN_PROGRESS;
    }

    public void markCompleted() {
        this.status = SessionStatus.COMPLETED;
        this.completedAt = OffsetDateTime.now();
    }

    public void markCancelled() {
        this.status = SessionStatus.CANCELLED;
    }

    public void markNoShow() {
        this.status = SessionStatus.NO_SHOW;
    }

    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public MentorProfile getMentor() { return mentor; }
    public UUID getStudentId() { return studentId; }
    public OffsetDateTime getScheduledAt() { return scheduledAt; }
    public int getDurationMinutes() { return durationMinutes; }
    public SessionMode getSessionMode() { return sessionMode; }
    public SessionStatus getStatus() { return status; }
    public String getMeetingLink() { return meetingLink; }
    public String getNotes() { return notes; }
    public long getVersion() { return version; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
}
