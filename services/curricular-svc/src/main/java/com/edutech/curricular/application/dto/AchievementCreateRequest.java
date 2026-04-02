package com.edutech.curricular.application.dto;

import java.time.LocalDate;

public record AchievementCreateRequest(String achievementType, String title, String description, LocalDate issuedAt) {}
