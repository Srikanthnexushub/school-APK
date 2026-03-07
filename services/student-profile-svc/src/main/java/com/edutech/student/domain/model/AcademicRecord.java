package com.edutech.student.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "academic_records", schema = "student_schema")
public class AcademicRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "academic_year", nullable = false)
    private Integer academicYear;

    @Column(name = "class_grade", nullable = false)
    private Integer classGrade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Board board;

    @Column(name = "percentage_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentageScore;

    @Column(precision = 4, scale = 2)
    private BigDecimal cgpa;

    @Column
    private String remarks;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private AcademicRecord() {}

    public static AcademicRecord create(UUID studentId,
                                        Integer academicYear,
                                        Integer classGrade,
                                        Board board,
                                        BigDecimal percentageScore,
                                        BigDecimal cgpa,
                                        String remarks) {
        AcademicRecord record = new AcademicRecord();
        record.studentId = studentId;
        record.academicYear = academicYear;
        record.classGrade = classGrade;
        record.board = board;
        record.percentageScore = percentageScore;
        record.cgpa = cgpa;
        record.remarks = remarks;
        record.createdAt = Instant.now();
        return record;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public Integer getAcademicYear() {
        return academicYear;
    }

    public Integer getClassGrade() {
        return classGrade;
    }

    public Board getBoard() {
        return board;
    }

    public BigDecimal getPercentageScore() {
        return percentageScore;
    }

    public BigDecimal getCgpa() {
        return cgpa;
    }

    public String getRemarks() {
        return remarks;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
