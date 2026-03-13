// src/main/java/com/edutech/auth/application/dto/RegisterRequest.java
package com.edutech.auth.application.dto;

import com.edutech.auth.domain.model.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 128)
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
             message = "Password must contain at least 1 uppercase letter, 1 digit, and 1 special character")
    String password,
    @NotNull Role role,
    UUID centerId,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @Size(max = 20) String phoneNumber,
    @NotBlank String captchaToken,
    @NotNull @Valid DeviceFingerprint deviceFingerprint,
    String parentEmail
) {}
