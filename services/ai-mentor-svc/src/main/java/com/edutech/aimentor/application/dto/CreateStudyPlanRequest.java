package com.edutech.aimentor.application.dto;

import com.edutech.aimentor.domain.model.PriorityLevel;
import com.edutech.aimentor.domain.model.SubjectArea;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateStudyPlanRequest(
        @NotNull UUID studentId,
        @NotNull UUID enrollmentId,
        @NotBlank String title,
        String description,
        LocalDate targetExamDate,
        @Valid List<StudyPlanItemRequest> items
) {
    public record StudyPlanItemRequest(
            @NotNull SubjectArea subjectArea,
            @NotBlank String topic,
            PriorityLevel priorityLevel
    ) {}
}
