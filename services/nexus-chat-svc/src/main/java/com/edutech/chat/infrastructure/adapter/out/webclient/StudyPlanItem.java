package com.edutech.chat.infrastructure.adapter.out.webclient;

import java.util.UUID;

public record StudyPlanItem(
    UUID id,
    String title,
    int totalItems,
    int completedItems,
    String targetExamDate
) {}
