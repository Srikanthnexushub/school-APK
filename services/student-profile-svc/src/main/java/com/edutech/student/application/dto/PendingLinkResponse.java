package com.edutech.student.application.dto;

import java.time.Instant;

public record PendingLinkResponse(
        String otp,
        String parentName,
        Instant expiresAt
) {}
