package com.edutech.curricular.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(schema = "curricular_schema", name = "academic_terms")
@Getter
@Setter
@NoArgsConstructor
public class AcademicTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(name = "term_name", nullable = false, length = 60)
    private String termName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    public static AcademicTerm create(AcademicYear academicYear, String termName, LocalDate startDate, LocalDate endDate) {
        AcademicTerm at = new AcademicTerm();
        at.academicYear = academicYear;
        at.termName = termName;
        at.startDate = startDate;
        at.endDate = endDate;
        return at;
    }
}
