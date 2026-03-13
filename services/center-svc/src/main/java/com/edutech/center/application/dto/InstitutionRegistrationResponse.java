// src/main/java/com/edutech/center/application/dto/InstitutionRegistrationResponse.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.CenterStatus;

import java.time.Instant;
import java.util.UUID;

public record InstitutionRegistrationResponse(
    UUID centerId,
    String name,
    String city,
    CenterStatus status,
    Instant createdAt,
    String warningMessage
) {}
