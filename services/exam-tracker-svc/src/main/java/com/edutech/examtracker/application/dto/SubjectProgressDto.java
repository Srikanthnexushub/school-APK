package com.edutech.examtracker.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record SubjectProgressDto(
        String subject,
        Integer totalTopics,
        Integer completedTopics,
        BigDecimal completionPercent,
        List<SyllabusModuleResponse> topics
) {}
