package com.edutech.student.application.dto;

import java.time.Instant;
import java.util.UUID;

public record GenerateLinkOtpResponse(
        UUID studentId,
        String studentName,
        Instant expiresAt
) {}
