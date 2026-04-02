package com.edutech.curricular.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ActivityEnrollmentResponse(UUID enrollmentId, UUID activityId, String activityName, String category, String status, Instant enrolledAt) {}
