// src/main/java/com/edutech/parent/application/dto/UpdateNotificationPreferenceRequest.java
package com.edutech.parent.application.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferenceRequest(@NotNull Boolean enabled) {}
