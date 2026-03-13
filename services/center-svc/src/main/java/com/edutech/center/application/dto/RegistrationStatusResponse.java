// src/main/java/com/edutech/center/application/dto/RegistrationStatusResponse.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.CenterStatus;

import java.time.Instant;
import java.util.UUID;

public record RegistrationStatusResponse(
    UUID centerId,
    String name,
    String code,
    String institutionType,
    String board,
    String city,
    String state,
    String phone,
    String email,
    CenterStatus status,
    String rejectionReason,
    UUID reviewedBy,
    Instant reviewedAt,
    Instant createdAt
) {}
