package com.edutech.examtracker.application.dto;

import com.edutech.examtracker.domain.model.ModuleStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSyllabusModuleRequest(
        @NotNull Integer completionPercent,
        @NotNull ModuleStatus status
) {}
