package com.edutech.curricular.application.dto;

import java.util.List;
import java.util.UUID;

public record ChildJourneyResponse(UUID studentId, List<LearningPathResponse> learningPaths, List<ActivityEnrollmentResponse> activities, List<AchievementResponse> achievements) {}
