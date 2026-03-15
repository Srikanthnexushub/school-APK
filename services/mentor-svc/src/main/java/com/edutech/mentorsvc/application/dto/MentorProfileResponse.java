package com.edutech.mentorsvc.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MentorProfileResponse(
        UUID id,
        UUID userId,
        String fullName,
        String email,
        String bio,
        String specializations,
        int yearsOfExperience,
        BigDecimal hourlyRate,
        boolean isAvailable,
        BigDecimal averageRating,
        int totalSessions,
        String gender,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
