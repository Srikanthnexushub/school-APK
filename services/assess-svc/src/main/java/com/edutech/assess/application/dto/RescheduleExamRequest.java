// src/main/java/com/edutech/assess/application/dto/RescheduleExamRequest.java
package com.edutech.assess.application.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RescheduleExamRequest(
        @NotNull Instant startAt,
        @NotNull Instant endAt
) {}
