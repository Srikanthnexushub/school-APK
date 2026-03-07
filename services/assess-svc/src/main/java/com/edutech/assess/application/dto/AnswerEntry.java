// src/main/java/com/edutech/assess/application/dto/AnswerEntry.java
package com.edutech.assess.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AnswerEntry(@NotNull UUID questionId, @Min(0) int selectedOption) {}
