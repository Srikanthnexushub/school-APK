// src/main/java/com/edutech/center/application/dto/BatchResponse.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.BatchStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BatchResponse(
    UUID id,
    UUID centerId,
    String name,
    String code,
    String subject,
    UUID teacherId,
    int maxStudents,
    int enrolledCount,
    LocalDate startDate,
    LocalDate endDate,
    BatchStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
