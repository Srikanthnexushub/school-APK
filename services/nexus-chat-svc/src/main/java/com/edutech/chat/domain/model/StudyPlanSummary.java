package com.edutech.chat.domain.model;

import java.util.UUID;

public record StudyPlanSummary(
    UUID planId,
    String title,
    int totalItems,
    int completedItems,
    String targetDate
) {}
