package com.edutech.chat.infrastructure.adapter.out.webclient;

import java.util.List;

public record GapAnalysisDto(
    ErsBreakdown ersBreakdown,
    List<SubjectGap> subjectGaps,
    List<PrerequisiteChain> prerequisiteChains,
    RiskSummary dropoutRisk
) {
    public static GapAnalysisDto empty() {
        return new GapAnalysisDto(null, List.of(), List.of(), null);
    }

    public record ErsBreakdown(
        double total,
        ErsComponent syllabusCoverage,
        ErsComponent mockTestTrend,
        ErsComponent masteryAverage,
        ErsComponent timeManagement,
        ErsComponent accuracy
    ) {}

    public record ErsComponent(double score, double weight, double gap) {}

    public record SubjectGap(
        String subject,
        double masteryPercent,
        String masteryLevel,
        double velocityPerWeek,
        String trend,
        double targetPercent,
        double gapPoints,
        String priority,
        List<String> topWeakTopics
    ) {}

    public record PrerequisiteChain(
        String blockingTopic,
        List<String> blockedTopics,
        String subject
    ) {}

    public record RiskSummary(String level, double score, String topFactor) {}
}
