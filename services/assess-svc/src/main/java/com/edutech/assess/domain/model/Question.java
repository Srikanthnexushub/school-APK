// src/main/java/com/edutech/assess/domain/model/Question.java
package com.edutech.assess.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "questions", schema = "assess_schema")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "exam_id", nullable = false, updatable = false)
    private UUID examId;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(name = "options_json", columnDefinition = "TEXT")
    private String optionsJson;

    @Column(name = "correct_answer", nullable = false)
    private int correctAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(nullable = false)
    private double marks;

    @Column
    private double difficulty;

    @Column
    private double discrimination;

    @Column(name = "guessing_param")
    private double guessingParam;

    /**
     * pgvector float array (1536 dims, OpenAI text-embedding-3-small).
     * Stored as a native vector column; NULL until generated via ai-gateway-svc.
     * The domain model uses a plain float[] — no pgvector dependency here.
     * The infrastructure adapter handles the JDBC type conversion.
     */
    @Column(columnDefinition = "vector(1536)")
    private float[] embedding;

    @Version
    private Long version;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private Question() {}

    public static Question create(UUID examId, String questionText, String optionsJson,
                                   int correctAnswer, String explanation, double marks,
                                   double difficulty, double discrimination, double guessingParam) {
        Question q = new Question();
        q.examId = examId;
        q.questionText = questionText;
        q.optionsJson = optionsJson;
        q.correctAnswer = correctAnswer;
        q.explanation = explanation;
        q.marks = marks;
        q.difficulty = difficulty;
        q.discrimination = discrimination;
        q.guessingParam = guessingParam;
        q.embedding = null;
        q.createdAt = Instant.now();
        q.updatedAt = Instant.now();
        return q;
    }

    /**
     * Reconstitutes a Question from a raw JDBC ResultSet row (used by similarity search adapter).
     * Does not set the embedding field — embedding is only needed for persistence, not domain logic.
     */
    public static Question reconstitute(UUID id, UUID examId, String questionText, String optionsJson,
                                        int correctAnswer, String explanation, double marks,
                                        double difficulty, double discrimination, double guessingParam,
                                        long version, Instant createdAt, Instant updatedAt, Instant deletedAt) {
        Question q = new Question();
        q.id = id;
        q.examId = examId;
        q.questionText = questionText;
        q.optionsJson = optionsJson;
        q.correctAnswer = correctAnswer;
        q.explanation = explanation;
        q.marks = marks;
        q.difficulty = difficulty;
        q.discrimination = discrimination;
        q.guessingParam = guessingParam;
        q.version = version;
        q.createdAt = createdAt;
        q.updatedAt = updatedAt;
        q.deletedAt = deletedAt;
        return q;
    }

    public UUID getId() { return id; }
    public UUID getExamId() { return examId; }
    public String getQuestionText() { return questionText; }
    public String getOptionsJson() { return optionsJson; }
    public int getCorrectAnswer() { return correctAnswer; }
    public String getExplanation() { return explanation; }
    public double getMarks() { return marks; }
    public double getDifficulty() { return difficulty; }
    public double getDiscrimination() { return discrimination; }
    public double getGuessingParam() { return guessingParam; }
    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
