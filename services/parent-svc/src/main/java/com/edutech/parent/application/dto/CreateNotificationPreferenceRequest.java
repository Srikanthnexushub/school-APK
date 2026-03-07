// src/main/java/com/edutech/parent/application/dto/CreateNotificationPreferenceRequest.java
package com.edutech.parent.application.dto;

import com.edutech.parent.domain.model.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNotificationPreferenceRequest(
        @NotNull NotificationChannel channel,
        @NotBlank String eventType,
        @NotNull Boolean enabled
) {}
