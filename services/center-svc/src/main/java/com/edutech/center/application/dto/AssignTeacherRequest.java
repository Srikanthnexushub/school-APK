// src/main/java/com/edutech/center/application/dto/AssignTeacherRequest.java
package com.edutech.center.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AssignTeacherRequest(
    @NotNull UUID userId,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotBlank @Email @Size(max = 255) String email,
    @Size(max = 20) String phoneNumber,
    @Size(max = 500) String subjects
) {}
