package com.edutech.curricular.application.dto;

import java.util.List;
import java.util.UUID;

public record CenterCoverageReport(UUID centerId, UUID subjectId, UUID gradeId, int totalTopics, int coveredTopics, double coveragePercent, List<String> uncoveredTopicCodes) {}
