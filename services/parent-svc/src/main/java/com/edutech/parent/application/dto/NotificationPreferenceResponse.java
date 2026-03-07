// src/main/java/com/edutech/parent/application/dto/NotificationPreferenceResponse.java
package com.edutech.parent.application.dto;

import com.edutech.parent.domain.model.NotificationChannel;

import java.time.Instant;
import java.util.UUID;

public record NotificationPreferenceResponse(
        UUID id,
        UUID parentId,
        NotificationChannel channel,
        String eventType,
        boolean enabled,
        Instant createdAt
) {}
