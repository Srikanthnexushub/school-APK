// src/main/java/com/edutech/assess/application/dto/QuestionResponse.java
package com.edutech.assess.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuestionResponse(
        UUID id,
        UUID examId,
        String questionText,
        List<String> options,
        int correctAnswer,
        String explanation,
        double marks,
        double difficulty,
        double discrimination,
        double guessingParam,
        Instant createdAt
) {}
