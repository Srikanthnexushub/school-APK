package com.edutech.aimentor.application.dto;

import com.edutech.aimentor.domain.model.SubjectArea;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubmitDoubtRequest(
        @NotNull UUID studentId,
        @NotNull UUID enrollmentId,
        @NotNull SubjectArea subjectArea,
        @NotBlank String question
) {}
