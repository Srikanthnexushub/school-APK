// src/main/java/com/edutech/assess/application/dto/EnrollmentResponse.java
package com.edutech.assess.application.dto;

import com.edutech.assess.domain.model.EnrollmentStatus;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentResponse(
        UUID id,
        UUID examId,
        UUID studentId,
        EnrollmentStatus status,
        Instant enrolledAt
) {}
