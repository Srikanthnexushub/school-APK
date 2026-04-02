package com.edutech.curricular.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "curricular_schema", name = "subjects")
@Getter
@Setter
@NoArgsConstructor
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "subject_code", nullable = false, length = 30)
    private String subjectCode;

    @Column(name = "subject_name", nullable = false, length = 120)
    private String subjectName;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public static Subject create(UUID boardId, String subjectCode, String subjectName) {
        Subject s = new Subject();
        s.boardId = boardId;
        s.subjectCode = subjectCode;
        s.subjectName = subjectName;
        s.active = true;
        s.createdAt = Instant.now();
        return s;
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }
}
