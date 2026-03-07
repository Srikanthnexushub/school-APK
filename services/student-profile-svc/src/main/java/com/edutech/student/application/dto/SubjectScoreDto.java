package com.edutech.student.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SubjectScoreDto(
        UUID id,
        String subjectName,
        String subjectCode,
        Integer marksObtained,
        Integer totalMarks,
        BigDecimal percentage
) {}
