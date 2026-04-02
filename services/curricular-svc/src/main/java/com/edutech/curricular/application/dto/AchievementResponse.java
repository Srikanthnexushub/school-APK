package com.edutech.curricular.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AchievementResponse(UUID id, UUID studentId, UUID activityId, String activityName, String achievementType, String title, LocalDate issuedAt) {}
