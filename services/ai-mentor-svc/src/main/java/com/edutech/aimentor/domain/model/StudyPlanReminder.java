package com.edutech.aimentor.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "study_plan_reminders", schema = "aimentor_schema")
public class StudyPlanReminder {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "study_plan_id", nullable = false)
    private UUID studyPlanId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "reminder_type", nullable = false, length = 30)
    private String reminderType;

    @Column(name = "remind_at", nullable = false)
    private Instant remindAt;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private boolean acknowledged;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    protected StudyPlanReminder() {}

    private StudyPlanReminder(UUID id, UUID studyPlanId, UUID studentId,
                               String reminderType, Instant remindAt,
                               String title, String message) {
        this.id = id;
        this.studyPlanId = studyPlanId;
        this.studentId = studentId;
        this.reminderType = reminderType;
        this.remindAt = remindAt;
        this.title = title;
        this.message = message;
        this.acknowledged = false;
        this.createdAt = Instant.now();
    }

    public static StudyPlanReminder create(UUID studyPlanId, UUID studentId,
                                           String reminderType, Instant remindAt,
                                           String title, String message) {
        return new StudyPlanReminder(UUID.randomUUID(), studyPlanId, studentId,
                reminderType, remindAt, title, message);
    }

    public void acknowledge() {
        this.acknowledged = true;
        this.acknowledgedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getStudyPlanId() { return studyPlanId; }
    public UUID getStudentId() { return studentId; }
    public String getReminderType() { return reminderType; }
    public Instant getRemindAt() { return remindAt; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isAcknowledged() { return acknowledged; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
