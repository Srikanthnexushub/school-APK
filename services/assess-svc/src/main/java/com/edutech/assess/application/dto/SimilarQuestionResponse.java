// src/main/java/com/edutech/assess/application/dto/SimilarQuestionResponse.java
package com.edutech.assess.application.dto;

import java.util.List;
import java.util.UUID;

public record SimilarQuestionResponse(
        UUID id,
        UUID examId,
        String questionText,
        List<String> options,
        double marks,
        double difficulty,
        double discrimination,
        double guessingParam
) {}
