// src/main/java/com/edutech/center/application/dto/ScheduleResponse.java
package com.edutech.center.application.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleResponse(
    UUID id,
    UUID batchId,
    UUID centerId,
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    String room,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    Instant createdAt
) {}
