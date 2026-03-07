// src/main/java/com/edutech/center/application/dto/CreateBatchRequest.java
package com.edutech.center.application.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateBatchRequest(
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Size(max = 50) String code,
    @NotBlank @Size(max = 100) String subject,
    UUID teacherId,
    @Min(1) @Max(200) int maxStudents,
    @NotNull LocalDate startDate,
    LocalDate endDate
) {}
