package com.edutech.aigateway.domain.model;

import java.util.List;

public record GeneratedQuestion(
        String questionText,
        List<String> options,
        int correctAnswer,
        String explanation,
        String difficulty
) {}
