// src/main/java/com/edutech/auth/application/dto/UserResponse.java
package com.edutech.auth.application.dto;

import com.edutech.auth.domain.model.Role;
import com.edutech.auth.domain.model.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String phoneNumber,
    Role role,
    UserStatus status,
    UUID centerId,
    Instant createdAt
) {}
