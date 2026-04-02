package com.edutech.curricular.application.dto;

import java.util.UUID;

public record GradeLevelResponse(UUID id, UUID boardId, String gradeCode, String displayName, int sortOrder) {}
