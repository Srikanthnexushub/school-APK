package com.edutech.curricular.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TopicResponse(UUID id, UUID chapterId, String topicCode, String topicTitle, String bloomsLevel, BigDecimal estHours, int sortOrder) {}
