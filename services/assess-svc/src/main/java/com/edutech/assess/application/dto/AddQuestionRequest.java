// src/main/java/com/edutech/assess/application/dto/AddQuestionRequest.java
package com.edutech.assess.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AddQuestionRequest(
        @NotBlank String questionText,
        @NotEmpty @Size(min = 2) List<String> options,
        @Min(0) int correctAnswer,
        String explanation,
        @DecimalMin("0.01") double marks,
        double difficulty,
        double discrimination,
        double guessingParam
) {}
