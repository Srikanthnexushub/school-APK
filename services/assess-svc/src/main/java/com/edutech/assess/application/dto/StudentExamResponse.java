// src/main/java/com/edutech/assess/application/dto/StudentExamResponse.java
package com.edutech.assess.application.dto;

import java.util.UUID;

public record StudentExamResponse(
        UUID id,
        String name,
        String description,
        String subject,
        int durationSeconds,
        int totalQuestions,
        String difficulty,
        String status,
        String startDate,
        UUID enrollmentId
) {}
