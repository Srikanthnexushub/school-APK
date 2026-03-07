// src/main/java/com/edutech/center/application/dto/AttendanceEntry.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttendanceEntry(
    @NotNull UUID studentId,
    @NotNull AttendanceStatus status,
    String notes
) {}
