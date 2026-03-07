package com.edutech.mentorsvc.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "session_feedback", schema = "mentor_schema")
public class SessionFeedback {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private MentorSession session;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "mentor_id", nullable = false)
    private UUID mentorId;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    protected SessionFeedback() {
    }

    private SessionFeedback(UUID id, MentorSession session, UUID studentId, UUID mentorId,
                            int rating, String comment) {
        this.id = id;
        this.session = session;
        this.studentId = studentId;
        this.mentorId = mentorId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = OffsetDateTime.now();
    }

    public static SessionFeedback create(MentorSession session, UUID studentId, UUID mentorId,
                                         int rating, String comment) {
        return new SessionFeedback(UUID.randomUUID(), session, studentId, mentorId, rating, comment);
    }

    public UUID getId() { return id; }
    public MentorSession getSession() { return session; }
    public UUID getStudentId() { return studentId; }
    public UUID getMentorId() { return mentorId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
