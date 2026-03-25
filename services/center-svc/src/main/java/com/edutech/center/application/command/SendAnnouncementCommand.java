// src/main/java/com/edutech/center/application/command/SendAnnouncementCommand.java
package com.edutech.center.application.command;

import com.edutech.center.domain.model.AnnouncementTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record SendAnnouncementCommand(
    @NotNull UUID centerId,
    @NotBlank @Size(max = 300) String title,
    @NotBlank String body,
    @NotNull AnnouncementTargetType targetType,
    UUID targetId,
    String targetRole,
    Instant scheduledAt,
    Instant expiresAt
) {}
