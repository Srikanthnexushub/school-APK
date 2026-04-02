package com.edutech.curricular.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "curricular_schema", name = "chapters")
@Getter
@Setter
@NoArgsConstructor
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "grade_id", nullable = false)
    private UUID gradeId;

    @Column(name = "chapter_number", nullable = false)
    private int chapterNumber;

    @Column(name = "chapter_title", nullable = false, length = 200)
    private String chapterTitle;

    @Column(name = "total_est_hours", precision = 6, scale = 2)
    private BigDecimal totalEstHours = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public static Chapter create(UUID subjectId, UUID gradeId, int chapterNumber, String chapterTitle, BigDecimal totalEstHours) {
        Chapter c = new Chapter();
        c.subjectId = subjectId;
        c.gradeId = gradeId;
        c.chapterNumber = chapterNumber;
        c.chapterTitle = chapterTitle;
        c.totalEstHours = totalEstHours != null ? totalEstHours : BigDecimal.ZERO;
        c.active = true;
        c.createdAt = Instant.now();
        return c;
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }
}
