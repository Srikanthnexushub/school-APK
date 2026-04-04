package com.edutech.chat.application.service;

import com.edutech.chat.domain.model.*;
import com.edutech.chat.infrastructure.adapter.out.webclient.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContextAggregatorService {

    private final StudentProfileWebClientAdapter profileClient;
    private final PerformanceWebClientAdapter performanceClient;
    private final AiMentorWebClientAdapter mentorClient;
    private final AssessWebClientAdapter assessClient;
    private final CenterWebClientAdapter centerClient;
    private final TeacherProfileWebClientAdapter teacherProfileClient;
    private final ParentProfileWebClientAdapter parentProfileClient;
    private final ExamTrackerWebClientAdapter examTrackerClient;
    private final GapAnalysisWebClientAdapter gapAnalysisClient;
    private final MentorGapCoverageWebClientAdapter mentorGapClient;

    @Value("${context.total-timeout-ms:800}")
    private long totalTimeoutMs;

    public RoleContext aggregate(UUID userId, String userRole, UUID centerId,
                                  String pageContext, String jwt) {
        return switch (userRole) {
            case "STUDENT" -> aggregateStudent(userId, pageContext, jwt);
            case "TEACHER" -> aggregateTeacher(userId, centerId, pageContext, jwt);
            case "CENTER_ADMIN", "INSTITUTION_ADMIN" -> aggregateAdmin(userId, centerId, pageContext, jwt);
            case "PARENT" -> aggregateParent(userId, pageContext, jwt);
            default -> {
                log.warn("Unknown role '{}' for userId={}, falling back to student context", userRole, userId);
                yield aggregateStudent(userId, pageContext, jwt);
            }
        };
    }

    // ─────────────────────────────────────────────
    // STUDENT — 8-service parallel aggregation
    // ─────────────────────────────────────────────
    private StudentContext aggregateStudent(UUID userId, String pageContext, String jwt) {
        try {
            Object[] results = Mono.zip(
                List.of(
                    profileClient.fetchProfile(userId, jwt)
                        .onErrorReturn(StudentProfileDto.empty(userId)),
                    performanceClient.fetchPerformance(userId, jwt)
                        .onErrorReturn(PerformanceDto.empty()),
                    mentorClient.fetchMentorContext(userId, jwt)
                        .onErrorReturn(MentorContextDto.empty()),
                    assessClient.fetchAssessContext(userId, jwt)
                        .onErrorReturn(AssessContextDto.empty()),
                    centerClient.fetchCenterContext(userId, jwt)
                        .onErrorReturn(CenterContextDto.empty()),
                    examTrackerClient.fetchVelocity(userId, jwt)
                        .onErrorReturn(ExamVelocityDto.empty()),
                    gapAnalysisClient.fetchGapAnalysis(userId, jwt)
                        .onErrorReturn(GapAnalysisDto.empty()),
                    mentorGapClient.fetchGapCoverage(userId, jwt)
                        .onErrorReturn(MentorGapCoverageDto.empty())
                ),
                arr -> arr
            ).block(Duration.ofMillis(totalTimeoutMs));

            if (results == null) {
                log.warn("Student context aggregation timed out for userId={}", userId);
                return StudentContext.empty(userId, pageContext);
            }
            return mapToStudentContext(results, userId, pageContext);

        } catch (Exception e) {
            log.error("Student context aggregation failed for userId={}: {}", userId, e.getMessage());
            return StudentContext.empty(userId, pageContext);
        }
    }

    // ─────────────────────────────────────────────
    // TEACHER — mentor profile only
    // ─────────────────────────────────────────────
    private TeacherContext aggregateTeacher(UUID userId, UUID centerId, String pageContext, String jwt) {
        try {
            TeacherProfileDto profile = teacherProfileClient.fetchProfile(userId, jwt)
                .block(Duration.ofMillis(totalTimeoutMs));

            if (profile == null) profile = TeacherProfileDto.empty();

            return new TeacherContext(
                userId,
                profile.fullName(),
                profile.bio(),
                profile.specializations() != null ? profile.specializations() : List.of(),
                null,   // centerName not fetched — avoid chained blocking call in hot path
                centerId,
                pageContext
            );
        } catch (Exception e) {
            log.error("Teacher context aggregation failed for userId={}: {}", userId, e.getMessage());
            return TeacherContext.empty(userId, centerId, pageContext);
        }
    }

    // ─────────────────────────────────────────────
    // ADMIN (CENTER_ADMIN / INSTITUTION_ADMIN)
    // ─────────────────────────────────────────────
    private AdminContext aggregateAdmin(UUID userId, UUID centerId, String pageContext, String jwt) {
        // For admin, we have centerId from JWT — no extra call needed for a functional prompt.
        // CenterName resolution would require a chained blocking call; deferred to future enhancement.
        return new AdminContext(userId, "Admin", null, centerId, pageContext);
    }

    // ─────────────────────────────────────────────
    // PARENT
    // ─────────────────────────────────────────────
    private ParentContext aggregateParent(UUID userId, String pageContext, String jwt) {
        try {
            ParentProfileDto profile = parentProfileClient.fetchProfile(userId, jwt)
                .block(Duration.ofMillis(totalTimeoutMs));

            if (profile == null) profile = ParentProfileDto.empty();

            return new ParentContext(
                userId,
                profile.fullName(),
                profile.linkedStudentNames() != null ? profile.linkedStudentNames() : List.of(),
                pageContext
            );
        } catch (Exception e) {
            log.error("Parent context aggregation failed for userId={}: {}", userId, e.getMessage());
            return ParentContext.empty(userId, pageContext);
        }
    }

    // ─────────────────────────────────────────────
    // Student context mapping
    // ─────────────────────────────────────────────
    private StudentContext mapToStudentContext(Object[] arr, UUID userId, String pageContext) {

        StudentProfileDto profile   = (StudentProfileDto) arr[0];
        PerformanceDto perf         = (PerformanceDto) arr[1];
        MentorContextDto mentor     = (MentorContextDto) arr[2];
        AssessContextDto assess     = (AssessContextDto) arr[3];
        CenterContextDto center     = (CenterContextDto) arr[4];
        ExamVelocityDto velocity    = (ExamVelocityDto) arr[5];
        GapAnalysisDto gap          = (GapAnalysisDto) arr[6];
        MentorGapCoverageDto mentorGap = (MentorGapCoverageDto) arr[7];

        List<WeakAreaSummary> weakAreas = perf.weakAreas().stream()
            .limit(3)
            .map(w -> new WeakAreaSummary(w.subject(), w.topicName(), w.masteryPercent(), w.severity()))
            .toList();

        List<MasterySummary> mastery = perf.subjectMastery().stream()
            .map(m -> new MasterySummary(m.subject(), m.masteryPercent(), m.masteryLevel()))
            .toList();

        return new StudentContext(
            userId,
            profile.fullName(),
            profile.currentClass(),
            profile.board(),
            profile.stream(),
            profile.subjects() != null ? profile.subjects() : List.of(),
            profile.targetYear(),
            perf.ersScore() != null ? BigDecimal.valueOf(perf.ersScore()) : BigDecimal.ZERO,
            resolveRisk(perf.ersScore()),
            weakAreas,
            mastery,
            mentor.activeStudyPlan() != null
                ? Optional.of(new StudyPlanSummary(
                    mentor.activeStudyPlan().id(),
                    mentor.activeStudyPlan().title(),
                    mentor.activeStudyPlan().totalItems(),
                    mentor.activeStudyPlan().completedItems(),
                    mentor.activeStudyPlan().targetExamDate()))
                : Optional.empty(),
            mentor.pendingDoubtCount(),
            assess.lastExam() != null
                ? Optional.of(new RecentExamSummary(
                    assess.lastExam().examId(),
                    assess.lastExam().title(),
                    assess.lastExam().scoredMarks(),
                    assess.lastExam().totalMarks(),
                    assess.lastExam().percentage(),
                    assess.lastExam().letterGrade(),
                    assess.lastExam().submittedAt()))
                : Optional.empty(),
            assess.examsThisMonth(),
            Optional.ofNullable(center.batchName()),
            Optional.ofNullable(center.centerName()),
            buildExamTimeline(velocity),
            buildGapSummary(gap),
            buildMentorCoverage(mentorGap),
            pageContext
        );
    }

    private Optional<String> buildExamTimeline(ExamVelocityDto velocity) {
        if (velocity.enrollments().isEmpty()) return Optional.empty();
        String timeline = velocity.enrollments().stream()
            .map(e -> {
                String status = e.onTrack()
                    ? "[ON_TRACK]"
                    : "[BEHIND " + e.behindByDays() + "d]";
                return String.format("%s: %dd left, %.0f%% syllabus %s",
                    e.examCode() != null ? e.examCode() : e.examName(),
                    e.daysRemaining(),
                    e.completionPercent(),
                    status);
            })
            .collect(java.util.stream.Collectors.joining("; "));
        return Optional.of(timeline);
    }

    private Optional<String> buildGapSummary(GapAnalysisDto gap) {
        if (gap.dropoutRisk() == null) return Optional.empty();
        String risk = String.format("Risk: %s (ERS %.1f)", gap.dropoutRisk().level(), gap.dropoutRisk().score());
        String bottleneck = gap.dropoutRisk().topFactor() != null
            ? " | Bottleneck: " + gap.dropoutRisk().topFactor()
            : "";
        String topGaps = gap.subjectGaps().stream()
            .filter(sg -> !sg.topWeakTopics().isEmpty())
            .limit(2)
            .map(sg -> sg.subject() + "\u2192" + sg.topWeakTopics().get(0))
            .collect(java.util.stream.Collectors.joining(", "));
        String gapStr = topGaps.isBlank() ? "" : " | Top gaps: " + topGaps;
        return Optional.of(risk + bottleneck + gapStr);
    }

    private Optional<String> buildMentorCoverage(MentorGapCoverageDto mentorGap) {
        if (mentorGap.totalCompletedSessions() == 0) return Optional.empty();
        return Optional.of(String.format("%d sessions, %d min total, last: %d days ago",
            mentorGap.totalCompletedSessions(),
            mentorGap.totalStudyMinutes(),
            mentorGap.daysSinceLastSession()));
    }

    private String resolveRisk(Double ers) {
        if (ers == null) return "UNKNOWN";
        if (ers < 40) return "AT_RISK";
        if (ers < 70) return "NEEDS_ATTENTION";
        if (ers < 90) return "ON_TRACK";
        return "EXCELLENT";
    }
}
