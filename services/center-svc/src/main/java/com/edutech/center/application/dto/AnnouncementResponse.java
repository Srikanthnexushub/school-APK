// src/main/java/com/edutech/center/application/dto/AnnouncementResponse.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.AnnouncementTargetType;

import java.time.Instant;
import java.util.UUID;

public record AnnouncementResponse(
    UUID id,
    UUID centerId,
    String title,
    String body,
    UUID sentById,
    String sentByRole,
    AnnouncementTargetType targetType,
    UUID targetId,
    String targetRole,
    Instant scheduledAt,
    Instant sentAt,
    Instant expiresAt,
    Instant createdAt
) {}
