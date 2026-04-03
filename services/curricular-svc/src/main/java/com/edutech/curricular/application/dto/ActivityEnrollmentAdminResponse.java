package com.edutech.curricular.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ActivityEnrollmentAdminResponse(UUID enrollmentId, UUID studentId, String status, Instant enrolledAt) {}
