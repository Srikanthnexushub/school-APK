package com.edutech.performance.application.dto;

import com.edutech.performance.domain.model.ErrorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record RecordWeakAreaRequest(
        @NotNull UUID enrollmentId,
        @NotBlank String subject,
        @NotBlank String topicName,
        String chapterName,
        @NotNull BigDecimal masteryPercent,
        @NotNull ErrorType primaryErrorType,
        @NotNull Integer incorrectAttempts,
        @NotNull Integer totalAttempts,
        Boolean prerequisitesWeak
) {
}
