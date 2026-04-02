package com.edutech.curricular.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ChapterResponse(UUID id, UUID subjectId, UUID gradeId, int chapterNumber, String chapterTitle, BigDecimal totalEstHours) {}
