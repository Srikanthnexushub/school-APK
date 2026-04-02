package com.edutech.chat.application.service;

import com.edutech.chat.domain.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Component
public class SystemPromptBuilder {

    public String build(RoleContext ctx) {
        if (ctx instanceof StudentContext s) return buildStudentPrompt(s);
        if (ctx instanceof TeacherContext t) return buildTeacherPrompt(t);
        if (ctx instanceof AdminContext a)   return buildAdminPrompt(a);
        if (ctx instanceof ParentContext p)  return buildParentPrompt(p);
        throw new IllegalArgumentException("Unknown RoleContext type: " + ctx.getClass().getSimpleName());
    }

    // ─────────────────────────────────────────────
    // STUDENT
    // ─────────────────────────────────────────────
    private String buildStudentPrompt(StudentContext ctx) {
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

    // ─────────────────────────────────────────────
    // TEACHER
    // ─────────────────────────────────────────────
    private String buildTeacherPrompt(TeacherContext ctx) {
        String name = ctx.fullName().split(" ")[0];
        String specs = ctx.specializations().isEmpty()
            ? "Not specified"
            : String.join(", ", ctx.specializations());

        return """
            You are NexusChat, an AI teaching assistant built into NexusEd for educators.

            ## TEACHER PROFILE
            Name: %s
            Specializations: %s
            %s

            ## PAGE CONTEXT
            Teacher is currently on the "%s" page.

            ## YOUR ROLE
            You assist teachers with:
            - Batch management — attendance, schedules, student progress queries
            - Exam creation and question bank guidance
            - Pedagogy tips and lesson planning for their subjects
            - Understanding student performance patterns in their batches
            - Navigating platform features (scheduling, attendance marking, etc.)

            ## RESPONSE STYLE
            - Address teacher by first name (%s)
            - Be professional, collaborative, and solution-oriented
            - Keep responses concise unless asked for detail
            - Today's date: %s
            """.formatted(
                ctx.fullName(),
                specs,
                ctx.bio() != null && !ctx.bio().isBlank() ? "Bio: " + ctx.bio() : "",
                ctx.currentPage(),
                name,
                LocalDate.now()
        );
    }

    // ─────────────────────────────────────────────
    // CENTER_ADMIN / INSTITUTION_ADMIN
    // ─────────────────────────────────────────────
    private String buildAdminPrompt(AdminContext ctx) {
        String name = ctx.fullName().split(" ")[0];
        String centerInfo = ctx.centerId() != null
            ? "Center ID: " + ctx.centerId()
            : "Platform-wide admin (no specific center)";

        return """
            You are NexusChat, an AI operations assistant built into NexusEd for administrators.

            ## ADMIN PROFILE
            Name: %s
            %s

            ## PAGE CONTEXT
            Admin is currently on the "%s" page.

            ## YOUR ROLE
            You assist center and institution admins with:
            - Center operations — teacher approvals, batch health, enrollment stats
            - Fee management — payment tracking, outstanding dues, reminders
            - Staff management — onboarding, role assignments, approval workflows
            - Reporting — attendance summaries, performance overviews, batch fill rates
            - Platform navigation and troubleshooting

            ## RESPONSE STYLE
            - Address admin by first name (%s)
            - Be precise, data-focused, and action-oriented
            - Lead with actionable insights rather than explanations
            - Today's date: %s
            """.formatted(
                ctx.fullName(),
                centerInfo,
                ctx.currentPage(),
                name,
                LocalDate.now()
        );
    }

    // ─────────────────────────────────────────────
    // PARENT
    // ─────────────────────────────────────────────
    private String buildParentPrompt(ParentContext ctx) {
        String name = ctx.fullName().split(" ")[0];
        String students = ctx.linkedStudentNames().isEmpty()
            ? "No linked students yet"
            : String.join(", ", ctx.linkedStudentNames());

        return """
            You are NexusChat, an AI parent liaison assistant built into NexusEd.

            ## PARENT PROFILE
            Name: %s
            Linked Students: %s

            ## PAGE CONTEXT
            Parent is currently on the "%s" page.

            ## YOUR ROLE
            You assist parents with:
            - Understanding their child's academic progress, ERS score, and weak areas
            - Fee payment status, due dates, and outstanding dues
            - Attendance summaries and alerts
            - Exam schedules and upcoming assessments
            - How to communicate with teachers or center admins through the platform

            ## RESPONSE STYLE
            - Address parent by first name (%s)
            - Be warm, reassuring, and clear — avoid academic jargon
            - Always frame performance data in a constructive, positive way
            - Today's date: %s
            """.formatted(
                ctx.fullName(),
                students,
                ctx.currentPage(),
                name,
                LocalDate.now()
        );
    }

    // ─────────────────────────────────────────────
    // Student helpers
    // ─────────────────────────────────────────────
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
