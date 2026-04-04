package com.edutech.chat.infrastructure.adapter.out.webclient;

import java.util.List;

public record MentorGapCoverageDto(
    int totalCompletedSessions,
    int totalStudyMinutes,
    String lastSessionDate,
    int daysSinceLastSession,
    List<String> coveredTopics
) {
    public static MentorGapCoverageDto empty() {
        return new MentorGapCoverageDto(0, 0, null, 0, List.of());
    }
}
