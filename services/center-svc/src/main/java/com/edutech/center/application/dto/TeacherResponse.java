// src/main/java/com/edutech/center/application/dto/TeacherResponse.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.TeacherStatus;

import java.time.Instant;
import java.util.UUID;

public record TeacherResponse(
    UUID id,
    UUID centerId,
    UUID userId,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    String subjects,
    TeacherStatus status,
    Instant joinedAt
) {}
