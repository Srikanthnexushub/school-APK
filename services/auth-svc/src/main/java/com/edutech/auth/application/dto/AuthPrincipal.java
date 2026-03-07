// src/main/java/com/edutech/auth/application/dto/AuthPrincipal.java
package com.edutech.auth.application.dto;

import com.edutech.auth.domain.model.Role;

import java.util.UUID;

/**
 * Immutable security principal stored in Spring SecurityContext after JWT validation.
 * Injected into controller methods via @AuthenticationPrincipal.
 */
public record AuthPrincipal(
    UUID userId,
    String email,
    Role role,
    UUID centerId,
    String deviceFingerprintHash
) {}
