package com.edutech.curricular.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "curricular_schema", name = "grade_levels")
@Getter
@Setter
@NoArgsConstructor
public class GradeLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "grade_code", nullable = false, length = 20)
    private String gradeCode;

    @Column(name = "display_name", nullable = false, length = 60)
    private String displayName;

    @Column(name = "sort_order")
    private int sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static GradeLevel create(UUID boardId, String gradeCode, String displayName, int sortOrder) {
        GradeLevel g = new GradeLevel();
        g.boardId = boardId;
        g.gradeCode = gradeCode;
        g.displayName = displayName;
        g.sortOrder = sortOrder;
        g.createdAt = Instant.now();
        return g;
    }
}
