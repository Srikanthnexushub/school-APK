// src/main/java/com/edutech/auth/application/dto/RefreshTokenRequest.java
package com.edutech.auth.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RefreshTokenRequest(
    @NotBlank String refreshToken,
    @NotNull @Valid DeviceFingerprint deviceFingerprint
) {}
