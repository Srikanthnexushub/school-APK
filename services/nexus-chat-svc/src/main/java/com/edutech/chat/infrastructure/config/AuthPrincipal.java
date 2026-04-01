package com.edutech.chat.infrastructure.config;

import java.util.UUID;

public record AuthPrincipal(
    UUID userId,
    String email,
    String role,
    UUID centerId
) {}
