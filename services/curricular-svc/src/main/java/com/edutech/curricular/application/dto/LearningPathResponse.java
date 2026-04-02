package com.edutech.curricular.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LearningPathResponse(UUID pathId, UUID studentId, UUID subjectId, UUID gradeId, Instant computedAt, List<LearningPathItemResponse> items) {}
