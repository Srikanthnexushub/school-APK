package com.edutech.curricular.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LearningPathItemResponse(UUID itemId, UUID topicId, String topicTitle, String bloomsLevel, int sequenceOrder, BigDecimal urgencyScore, String status) {}
