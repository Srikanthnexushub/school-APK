// src/main/java/com/edutech/auth/application/dto/LoginRequest.java
package com.edutech.auth.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotBlank String captchaToken,
    @NotNull @Valid DeviceFingerprint deviceFingerprint
) {}
