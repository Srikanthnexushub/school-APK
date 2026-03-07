package com.edutech.student.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subject_scores", schema = "student_schema")
public class SubjectScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "academic_record_id", nullable = false, updatable = false)
    private UUID academicRecordId;

    @Column(name = "subject_name", nullable = false)
    private String subjectName;

    @Column(name = "subject_code")
    private String subjectCode;

    @Column(name = "marks_obtained", nullable = false)
    private Integer marksObtained;

    @Column(name = "total_marks", nullable = false)
    private Integer totalMarks;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private SubjectScore() {}

    public static SubjectScore create(UUID academicRecordId,
                                      String subjectName,
                                      String subjectCode,
                                      Integer marksObtained,
                                      Integer totalMarks) {
        SubjectScore score = new SubjectScore();
        score.academicRecordId = academicRecordId;
        score.subjectName = subjectName;
        score.subjectCode = subjectCode;
        score.marksObtained = marksObtained;
        score.totalMarks = totalMarks;
        score.percentage = totalMarks > 0
                ? BigDecimal.valueOf(marksObtained * 100.0 / totalMarks).setScale(2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        score.createdAt = Instant.now();
        return score;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAcademicRecordId() {
        return academicRecordId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public Integer getMarksObtained() {
        return marksObtained;
    }

    public Integer getTotalMarks() {
        return totalMarks;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
