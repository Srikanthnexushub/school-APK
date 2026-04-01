package com.edutech.chat.infrastructure.adapter.out.webclient;

public record MentorContextDto(
    StudyPlanItem activeStudyPlan,
    long pendingDoubtCount
) {
    public static MentorContextDto empty() {
        return new MentorContextDto(null, 0);
    }
}
