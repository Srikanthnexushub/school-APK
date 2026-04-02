package com.edutech.curricular.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "curricular_schema", name = "learning_paths")
@Getter
@Setter
@NoArgsConstructor
public class LearningPath {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "grade_id")
    private UUID gradeId;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    @Column(name = "next_review_at")
    private Instant nextReviewAt;

    @Column(name = "mastery_source", length = 30)
    private String masterySource = "PERFORMANCE_SVC";

    @Version
    @Column(name = "version")
    private Long version = 0L;

    public static LearningPath create(UUID studentId, UUID subjectId, UUID gradeId) {
        LearningPath lp = new LearningPath();
        lp.studentId = studentId;
        lp.subjectId = subjectId;
        lp.gradeId = gradeId;
        lp.computedAt = Instant.now();
        lp.masterySource = "PERFORMANCE_SVC";
        return lp;
    }

    public void refreshComputedAt() {
        this.computedAt = Instant.now();
    }
}
