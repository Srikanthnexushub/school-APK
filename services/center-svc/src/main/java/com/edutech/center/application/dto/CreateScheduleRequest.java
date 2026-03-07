// src/main/java/com/edutech/center/application/dto/CreateScheduleRequest.java
package com.edutech.center.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateScheduleRequest(
    @NotNull DayOfWeek dayOfWeek,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    @NotBlank @Size(max = 100) String room,
    @NotNull LocalDate effectiveFrom,
    LocalDate effectiveTo
) {}
