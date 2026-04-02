package com.edutech.chat.application.service;

import com.edutech.chat.domain.model.*;
import com.edutech.chat.infrastructure.adapter.out.webclient.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple5;

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
    // STUDENT — 5-service parallel aggregation
    // ─────────────────────────────────────────────
    private StudentContext aggregateStudent(UUID userId, String pageContext, String jwt) {
        try {
            Tuple5<StudentProfileDto, PerformanceDto, MentorContextDto, AssessContextDto, CenterContextDto> tuple =
                Mono.zip(
                    profileClient.fetchProfile(userId, jwt)
                        .onErrorReturn(StudentProfileDto.empty(userId)),
                    performanceClient.fetchPerformance(userId, jwt)
                        .onErrorReturn(PerformanceDto.empty()),
                    mentorClient.fetchMentorContext(userId, jwt)
                        .onErrorReturn(MentorContextDto.empty()),
                    assessClient.fetchAssessContext(userId, jwt)
                        .onErrorReturn(AssessContextDto.empty()),
                    centerClient.fetchCenterContext(userId, jwt)
                        .onErrorReturn(CenterContextDto.empty())
                )
                .block(Duration.ofMillis(totalTimeoutMs));

            if (tuple == null) {
                log.warn("Student context aggregation timed out for userId={}", userId);
                return StudentContext.empty(userId, pageContext);
            }
            return mapToStudentContext(tuple, userId, pageContext);

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
    // Student context mapping (unchanged logic)
    // ─────────────────────────────────────────────
    private StudentContext mapToStudentContext(
            Tuple5<StudentProfileDto, PerformanceDto, MentorContextDto, AssessContextDto, CenterContextDto> t,
            UUID userId, String pageContext) {

        StudentProfileDto profile = t.getT1();
        PerformanceDto perf = t.getT2();
        MentorContextDto mentor = t.getT3();
        AssessContextDto assess = t.getT4();
        CenterContextDto center = t.getT5();

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
            pageContext
        );
    }

    private String resolveRisk(Double ers) {
        if (ers == null) return "UNKNOWN";
        if (ers < 40) return "AT_RISK";
        if (ers < 70) return "NEEDS_ATTENTION";
        if (ers < 90) return "ON_TRACK";
        return "EXCELLENT";
    }
}
