package com.edutech.examtracker.application.dto;

import com.edutech.examtracker.domain.model.ExamCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SyllabusProgressResponse(
        UUID enrollmentId,
        ExamCode examCode,
        List<SubjectProgressDto> subjects,
        BigDecimal overallPercent,
        Integer totalModules,
        Integer completedModules
) {}
