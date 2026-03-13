package com.edutech.student.application.dto;

import java.util.UUID;

public record VerifyLinkOtpResponse(
        UUID studentId,
        String studentName
) {}
