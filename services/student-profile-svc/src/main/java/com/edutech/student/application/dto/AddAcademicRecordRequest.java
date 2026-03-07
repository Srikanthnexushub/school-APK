package com.edutech.student.application.dto;

import com.edutech.student.domain.model.Board;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddAcademicRecordRequest(
        @NotNull Integer academicYear,
        @NotNull Integer classGrade,
        Board board,
        @DecimalMin("0") @DecimalMax("100") BigDecimal percentageScore,
        BigDecimal cgpa
) {}
