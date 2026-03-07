// src/main/java/com/edutech/center/application/dto/UpdateBatchRequest.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.BatchStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateBatchRequest(
    UUID teacherId,
    @NotNull BatchStatus status
) {}
