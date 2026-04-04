package com.edutech.chat.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record StudentContext(
    UUID userId,
    String fullName,
    String currentClass,
    String board,
    String stream,
    List<String> subjects,
    int targetYear,
    BigDecimal ersScore,
    String ersRisk,
    List<WeakAreaSummary> weakAreas,
    List<MasterySummary> subjectMastery,
    Optional<StudyPlanSummary> activeStudyPlan,
    long pendingDoubtCount,
    Optional<RecentExamSummary> lastExam,
    int examsThisMonth,
    Optional<String> batchName,
    Optional<String> centerName,
    Optional<String> examTimeline,
    Optional<String> gapSummary,
    Optional<String> mentorCoverage,
    String currentPage
) implements RoleContext {

    @Override
    public String role() { return "STUDENT"; }

    public static StudentContext empty(UUID userId, String currentPage) {
        return new StudentContext(userId, "Student", "Unknown", "CBSE", null,
            List.of(), 2026, BigDecimal.ZERO, "UNKNOWN", List.of(), List.of(),
            Optional.empty(), 0, Optional.empty(), 0,
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            currentPage);
    }
}
