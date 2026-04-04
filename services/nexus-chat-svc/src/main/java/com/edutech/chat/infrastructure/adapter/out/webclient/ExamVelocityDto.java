package com.edutech.chat.infrastructure.adapter.out.webclient;

import java.util.List;

public record ExamVelocityDto(
    List<EnrollmentSummary> enrollments
) {
    public static ExamVelocityDto empty() { return new ExamVelocityDto(List.of()); }

    public record EnrollmentSummary(
        String examName,
        String examCode,
        int daysRemaining,
        double completionPercent,
        boolean onTrack,
        int behindByDays,
        int mockTestCount,
        double latestScorePercent,
        String mockTrend,
        double studyHoursThisWeek
    ) {}
}
