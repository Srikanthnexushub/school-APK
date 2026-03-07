// src/main/java/com/edutech/auth/application/dto/BiometricsRequest.java
package com.edutech.auth.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BiometricsRequest(
    @NotEmpty List<KeystrokeEvent> keystrokes,
    @NotBlank String sessionId
) {}
