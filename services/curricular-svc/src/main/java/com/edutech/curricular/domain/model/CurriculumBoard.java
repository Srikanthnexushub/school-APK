package com.edutech.curricular.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "curricular_schema", name = "curriculum_boards")
@Getter
@Setter
@NoArgsConstructor
public class CurriculumBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "board_code", unique = true, nullable = false, length = 30)
    private String boardCode;

    @Column(name = "board_name", nullable = false, length = 120)
    private String boardName;

    @Column(name = "country_code", length = 2)
    private String countryCode = "IN";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public static CurriculumBoard create(String boardCode, String boardName) {
        CurriculumBoard b = new CurriculumBoard();
        b.boardCode = boardCode;
        b.boardName = boardName;
        b.countryCode = "IN";
        b.active = true;
        b.createdAt = Instant.now();
        return b;
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }
}
