// src/main/java/com/edutech/center/application/dto/AttendanceResponse.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.AttendanceStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AttendanceResponse(
    UUID id,
    UUID batchId,
    UUID studentId,
    LocalDate date,
    AttendanceStatus status,
    UUID markedByTeacherId,
    String notes,
    Instant createdAt
) {}
