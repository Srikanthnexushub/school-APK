package com.edutech.curricular.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(schema = "curricular_schema", name = "academic_years")
@Getter
@Setter
@NoArgsConstructor
public class AcademicYear {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "center_id", nullable = false)
    private UUID centerId;

    @Column(name = "board_id")
    private UUID boardId;

    @Column(name = "grade_id")
    private UUID gradeId;

    @Column(name = "year_label", nullable = false, length = 20)
    private String yearLabel;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "academicYear", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AcademicTerm> terms = new ArrayList<>();

    public static AcademicYear create(UUID centerId, UUID boardId, UUID gradeId, String yearLabel, LocalDate startDate, LocalDate endDate) {
        AcademicYear ay = new AcademicYear();
        ay.centerId = centerId;
        ay.boardId = boardId;
        ay.gradeId = gradeId;
        ay.yearLabel = yearLabel;
        ay.startDate = startDate;
        ay.endDate = endDate;
        ay.active = true;
        ay.createdAt = Instant.now();
        return ay;
    }

    public void deactivate() {
        this.active = false;
    }
}
