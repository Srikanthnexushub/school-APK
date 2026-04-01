package com.edutech.chat.application.service;

import com.edutech.chat.domain.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Component
public class SystemPromptBuilder {

    public String build(StudentContext ctx) {
        return """
            You are NexusChat, an intelligent AI academic assistant built into NexusEd platform.
            You have access to this student's real-time academic data below. Use it proactively.

            ## STUDENT PROFILE
            Name: %s | Class: %s | Board: %s | Stream: %s | Target Year: %d
            Subjects: %s

            ## CURRENT PERFORMANCE
            Exam Readiness Score (ERS): %.1f/100 — %s
            Subject Mastery:
            %s

            ## WEAK AREAS (requires attention)
            %s

            ## RECENT ACTIVITY
            Last Exam: %s
            Active Study Plan: %s
            Pending Doubts: %d
            Exams This Month: %d

            ## ENROLLMENT
            %s

            ## PAGE CONTEXT
            Student is currently on the "%s" page. Tailor your response to this context.

            ## CAPABILITIES
            You can answer academic doubts, explain concepts, help with exam prep,
            and analyse this student's performance data above.

            You can EXECUTE ACTIONS by including a JSON block at the end of your response:
            {"action": "CREATE_STUDY_PLAN", "params": {"subject": "Physics", "days": 7}}
            {"action": "SCHEDULE_REMINDER", "params": {"message": "Review Newton's Laws", "dueInHours": 24}}
            {"action": "NAVIGATE", "params": {"path": "/performance", "label": "Performance page"}}
            {"action": "SHOW_WEAK_AREAS", "params": {}}
            Only include the action JSON when it truly helps the student. Never fabricate data.

            ## RESPONSE STYLE
            - Address student by first name (%s)
            - Be warm, encouraging, never judgmental about low scores
            - Keep responses focused (2-3 paragraphs max unless asked for detail)
            - For weak areas, always suggest specific, actionable study techniques
            - If student sounds stressed or frustrated, acknowledge it first
            - Today's date: %s
            """.formatted(
                ctx.fullName(), ctx.currentClass(), ctx.board(),
                ctx.stream() != null ? ctx.stream() : "Not set",
                ctx.targetYear(),
                ctx.subjects().isEmpty() ? "Not set" : String.join(", ", ctx.subjects()),
                ctx.ersScore().doubleValue(), ctx.ersRisk(),
                formatMastery(ctx),
                formatWeakAreas(ctx),
                formatLastExam(ctx),
                formatStudyPlan(ctx),
                ctx.pendingDoubtCount(),
                ctx.examsThisMonth(),
                formatEnrollment(ctx),
                ctx.currentPage(),
                ctx.fullName().split(" ")[0],
                LocalDate.now()
        );
    }

    private String formatMastery(StudentContext ctx) {
        if (ctx.subjectMastery().isEmpty()) return "  No mastery data yet";
        return ctx.subjectMastery().stream()
            .map(m -> "  - " + m.subject() + ": " + String.format("%.1f", m.masteryPercent()) +
                      "% (" + m.masteryLevel() + ")")
            .collect(Collectors.joining("\n"));
    }

    private String formatWeakAreas(StudentContext ctx) {
        if (ctx.weakAreas().isEmpty()) return "  No critical weak areas detected";
        return ctx.weakAreas().stream()
            .map(w -> "  - " + w.subject() + " \u2192 " + w.topic() +
                      " (" + String.format("%.1f", w.masteryPercent()) + "% mastery, " + w.severity() + ")")
            .collect(Collectors.joining("\n"));
    }

    private String formatLastExam(StudentContext ctx) {
        return ctx.lastExam()
            .map(e -> e.title() + " \u2014 " + String.format("%.0f", e.percentage()) +
                      "% (" + e.letterGrade() + "), submitted " + e.submittedAt().toString().substring(0, 10))
            .orElse("No exams taken yet");
    }

    private String formatStudyPlan(StudentContext ctx) {
        return ctx.activeStudyPlan()
            .map(p -> p.title() + " \u2014 " + p.completedItems() + "/" + p.totalItems() + " items complete")
            .orElse("No active study plan");
    }

    private String formatEnrollment(StudentContext ctx) {
        String batch = ctx.batchName().orElse("Not enrolled in a batch");
        String center = ctx.centerName().orElse("No center linked");
        return "Batch: " + batch + " | Center: " + center;
    }
}
