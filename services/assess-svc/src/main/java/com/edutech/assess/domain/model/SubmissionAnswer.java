// src/main/java/com/edutech/assess/domain/model/SubmissionAnswer.java
package com.edutech.assess.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "submission_answers", schema = "assess_schema")
public class SubmissionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "submission_id", nullable = false, updatable = false)
    private UUID submissionId;

    @Column(name = "question_id", nullable = false, updatable = false)
    private UUID questionId;

    @Column(name = "selected_option", nullable = false, updatable = false)
    private int selectedOption;

    @Column(name = "is_correct", nullable = false, updatable = false)
    private boolean isCorrect;

    @Column(name = "marks_awarded", nullable = false, updatable = false)
    private double marksAwarded;

    @Column(name = "answered_at", nullable = false, updatable = false)
    private Instant answeredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private SubmissionAnswer() {}

    public static SubmissionAnswer mark(UUID submissionId, UUID questionId, int selectedOption,
                                         boolean isCorrect, double marksAwarded) {
        SubmissionAnswer answer = new SubmissionAnswer();
        answer.submissionId = submissionId;
        answer.questionId = questionId;
        answer.selectedOption = selectedOption;
        answer.isCorrect = isCorrect;
        answer.marksAwarded = marksAwarded;
        answer.answeredAt = Instant.now();
        answer.createdAt = Instant.now();
        return answer;
    }

    public UUID getId() { return id; }
    public UUID getSubmissionId() { return submissionId; }
    public UUID getQuestionId() { return questionId; }
    public int getSelectedOption() { return selectedOption; }
    public boolean isCorrect() { return isCorrect; }
    public double getMarksAwarded() { return marksAwarded; }
    public Instant getAnsweredAt() { return answeredAt; }
    public Instant getCreatedAt() { return createdAt; }
}
