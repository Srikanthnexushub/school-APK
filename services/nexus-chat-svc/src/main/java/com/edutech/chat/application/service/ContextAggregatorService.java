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

    @Value("${context.total-timeout-ms:800}")
    private long totalTimeoutMs;

    public StudentContext aggregate(UUID userId, String userRole, String pageContext, String jwt) {
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
                log.warn("Context aggregation timed out for userId={}", userId);
                return StudentContext.empty(userId, pageContext);
            }

            return mapToStudentContext(tuple, userId, pageContext);

        } catch (Exception e) {
            log.error("Context aggregation failed for userId={}: {}", userId, e.getMessage());
            return StudentContext.empty(userId, pageContext);
        }
    }

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
