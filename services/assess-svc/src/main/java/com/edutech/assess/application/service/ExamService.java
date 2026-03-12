// src/main/java/com/edutech/assess/application/service/ExamService.java
package com.edutech.assess.application.service;

import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.CreateExamRequest;
import com.edutech.assess.application.dto.ExamResponse;
import com.edutech.assess.application.dto.StudentExamResponse;
import com.edutech.assess.application.exception.AssessAccessDeniedException;
import com.edutech.assess.application.exception.ExamNotFoundException;
import com.edutech.assess.domain.event.ExamPublishedEvent;
import com.edutech.assess.domain.model.Exam;
import com.edutech.assess.domain.model.ExamEnrollment;
import com.edutech.assess.domain.model.EnrollmentStatus;
import com.edutech.assess.domain.model.ExamStatus;
import com.edutech.assess.domain.port.in.CreateExamUseCase;
import com.edutech.assess.domain.port.in.ListPublishedExamsUseCase;
import com.edutech.assess.domain.port.in.PublishExamUseCase;
import com.edutech.assess.domain.port.out.AssessEventPublisher;
import com.edutech.assess.domain.port.out.ExamEnrollmentRepository;
import com.edutech.assess.domain.port.out.ExamRepository;
import com.edutech.assess.domain.port.out.NotificationEventPort;
import com.edutech.assess.domain.port.out.QuestionRepository;
import com.edutech.events.notification.NotificationSendEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExamService implements CreateExamUseCase, PublishExamUseCase, ListPublishedExamsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExamService.class);

    private final ExamRepository examRepository;
    private final ExamEnrollmentRepository enrollmentRepository;
    private final AssessEventPublisher eventPublisher;
    private final QuestionRepository questionRepository;
    private final NotificationEventPort notificationEventPort;

    public ExamService(ExamRepository examRepository,
                       ExamEnrollmentRepository enrollmentRepository,
                       AssessEventPublisher eventPublisher,
                       QuestionRepository questionRepository,
                       NotificationEventPort notificationEventPort) {
        this.examRepository = examRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.eventPublisher = eventPublisher;
        this.questionRepository = questionRepository;
        this.notificationEventPort = notificationEventPort;
    }

    @Override
    public ExamResponse createExam(CreateExamRequest request, AuthPrincipal principal) {
        if (!principal.belongsToCenter(request.centerId())) {
            throw new AssessAccessDeniedException();
        }
        Exam exam = Exam.create(
                request.batchId(),
                request.centerId(),
                request.title(),
                request.description(),
                request.mode(),
                request.durationMinutes(),
                request.maxAttempts(),
                request.startAt(),
                request.endAt(),
                request.totalMarks(),
                request.passingMarks()
        );
        Exam saved = examRepository.save(exam);
        log.info("Exam created: id={} batchId={}", saved.getId(), saved.getBatchId());
        return toResponse(saved);
    }

    @Override
    public ExamResponse publishExam(UUID examId, AuthPrincipal principal) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        if (!principal.belongsToCenter(exam.getCenterId())) {
            throw new AssessAccessDeniedException();
        }
        exam.publish();
        Exam saved = examRepository.save(exam);
        eventPublisher.publish(new ExamPublishedEvent(
                saved.getId(), saved.getBatchId(), saved.getCenterId(),
                saved.getTitle(), saved.getTotalMarks()
        ));
        // Fan out IN_APP notifications to all currently enrolled students
        List<ExamEnrollment> enrollments = enrollmentRepository.findByExamId(saved.getId());
        for (ExamEnrollment enrollment : enrollments) {
            publishExamAnnouncedNotification(saved, enrollment.getStudentId());
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ExamResponse getExam(UUID examId, AuthPrincipal principal) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        boolean isEnrolledStudent = principal.isStudent() && exam.getStatus() == ExamStatus.PUBLISHED;
        if (!principal.belongsToCenter(exam.getCenterId()) && !isEnrolledStudent) {
            throw new AssessAccessDeniedException();
        }
        return toResponse(exam);
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> listByBatch(UUID batchId, AuthPrincipal principal) {
        return examRepository.findByBatchId(batchId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ExamResponse> listByBatch(UUID batchId, AuthPrincipal principal, Pageable pageable) {
        List<ExamResponse> all = examRepository.findByBatchId(batchId).stream()
                .map(this::toResponse).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(start < all.size() ? all.subList(start, end) : List.of(), pageable, all.size());
    }

    @Transactional(readOnly = true)
    public Page<ExamResponse> listByCenter(UUID centerId, Pageable pageable) {
        List<ExamResponse> all = examRepository.findByCenterId(centerId).stream()
                .map(this::toResponse).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(start < all.size() ? all.subList(start, end) : List.of(), pageable, all.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentExamResponse> listPublishedExams(UUID studentId) {
        List<Exam> published = examRepository.findAllPublished();
        Map<UUID, ExamEnrollment> enrollmentByExamId = enrollmentRepository.findByStudentId(studentId)
                .stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED || e.getStatus() == EnrollmentStatus.COMPLETED)
                .collect(Collectors.toMap(ExamEnrollment::getExamId, Function.identity(),
                        (existing, replacement) -> existing));
        return published.stream()
                .map(exam -> toStudentResponse(exam, enrollmentByExamId.get(exam.getId()),
                        questionRepository.countByExamId(exam.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<StudentExamResponse> listPublishedExams(UUID studentId, Pageable pageable) {
        Map<UUID, ExamEnrollment> enrollmentByExamId = enrollmentRepository.findByStudentId(studentId)
                .stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED || e.getStatus() == EnrollmentStatus.COMPLETED)
                .collect(Collectors.toMap(ExamEnrollment::getExamId, Function.identity(),
                        (existing, replacement) -> existing));
        List<StudentExamResponse> all = examRepository.findAllPublished().stream()
                .map(exam -> toStudentResponse(exam, enrollmentByExamId.get(exam.getId()),
                        questionRepository.countByExamId(exam.getId())))
                .toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(start < all.size() ? all.subList(start, end) : List.of(), pageable, all.size());
    }

    /** Publishes EXAM_ANNOUNCED + STUDY_ROUTE notifications for a student. */
    private void publishExamAnnouncedNotification(Exam exam, UUID studentId) {
        String examDate = exam.getStartAt() != null
                ? DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneOffset.UTC).format(exam.getStartAt())
                : "TBD";
        notificationEventPort.publish(NotificationSendEvent.inApp(
                studentId,
                "New Exam: " + exam.getTitle(),
                "Exam \"" + exam.getTitle() + "\" is now available. Scheduled for " + examDate
                        + ". Duration: " + exam.getDurationMinutes() + " min | Total marks: " + (int) exam.getTotalMarks() + ".",
                Map.of("notificationType", "EXAM_ANNOUNCED",
                       "actionUrl",        "/assessments",
                       "examId",           exam.getId().toString())
        ));
        notificationEventPort.publish(NotificationSendEvent.inApp(
                studentId,
                "Study Route for " + exam.getTitle(),
                buildStudyRoute(exam),
                Map.of("notificationType", "STUDY_ROUTE",
                       "actionUrl",        "/assessments",
                       "examId",           exam.getId().toString())
        ));
    }

    /** Generates a structured week-by-week study plan based on days until exam. */
    private String buildStudyRoute(Exam exam) {
        long daysUntil = exam.getStartAt() != null
                ? ChronoUnit.DAYS.between(Instant.now(), exam.getStartAt())
                : 14;
        String examTitle = exam.getTitle();

        if (daysUntil <= 3) {
            return "Phase 1: Final Sprint (Days 1-" + daysUntil + ")\n"
                 + "• Revise all key formulas and definitions\n"
                 + "• Attempt one full mock exam\n"
                 + "• Focus only on high-weightage topics\n"
                 + "• Rest 8 hours before exam day";
        }
        if (daysUntil <= 7) {
            return "Phase 1: Foundation Scan (Days 1-2)\n"
                 + "• Read chapter summaries for " + examTitle + "\n"
                 + "• Identify your top 3 weak topics from previous tests\n"
                 + "• List all key formulas\n"
                 + "---\n"
                 + "Phase 2: Focused Practice (Days 3-" + (daysUntil - 1) + ")\n"
                 + "• Solve 15 practice questions per weak topic daily\n"
                 + "• Review every mistake immediately\n"
                 + "• Attempt one timed mock test\n"
                 + "---\n"
                 + "Phase 3: Final Revision (Day " + daysUntil + ")\n"
                 + "• Quick formula revision\n"
                 + "• Rest well — avoid new topics";
        }
        long phase1End = Math.max(2, daysUntil / 3);
        long phase2End = Math.max(phase1End + 2, (daysUntil * 2) / 3);
        return "Phase 1: Foundation (Days 1-" + phase1End + ")\n"
             + "• Read all chapters and mark key concepts for " + examTitle + "\n"
             + "• Build formula sheets and definition lists\n"
             + "• Identify weak areas from past performance\n"
             + "---\n"
             + "Phase 2: Deep Practice (Days " + (phase1End + 1) + "-" + phase2End + ")\n"
             + "• Solve 20 questions/day on weak topics\n"
             + "• Review each incorrect answer for root cause\n"
             + "• Attempt two full-length timed mock exams\n"
             + "• Peer/mentor review of difficult problems\n"
             + "---\n"
             + "Phase 3: Mock & Gap-Fill (Days " + (phase2End + 1) + "-" + (daysUntil - 2) + ")\n"
             + "• Full mock exam under exam conditions\n"
             + "• Analyse score breakdown — target topics below 50%\n"
             + "• Redo weak sections with focused practice sets\n"
             + "---\n"
             + "Phase 4: Final Sprint (Last 2 days)\n"
             + "• Revise formula sheets and high-weightage topics only\n"
             + "• No new material\n"
             + "• 8 hours of sleep before exam day";
    }

    private ExamResponse toResponse(Exam e) {
        int qCount = e.getId() != null ? questionRepository.countByExamId(e.getId()) : 0;
        return new ExamResponse(
                e.getId(), e.getBatchId(), e.getCenterId(), e.getTitle(), e.getDescription(),
                e.getMode(), e.getDurationMinutes(), e.getMaxAttempts(), e.getStartAt(), e.getEndAt(),
                e.getTotalMarks(), e.getPassingMarks(), e.getStatus(), e.getCreatedAt(),
                qCount
        );
    }

    private StudentExamResponse toStudentResponse(Exam exam, ExamEnrollment enrollment, int questionCount) {
        String status;
        UUID enrollmentId = null;
        if (enrollment != null && enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
            status = "COMPLETED";
            enrollmentId = enrollment.getId();
        } else if (enrollment != null) {
            status = "ENROLLED";
            enrollmentId = enrollment.getId();
        } else if (exam.getStartAt() != null && exam.getStartAt().isAfter(Instant.now())) {
            status = "UPCOMING";
        } else {
            status = "AVAILABLE";
        }
        String startDate = exam.getStartAt() != null ? exam.getStartAt().toString() : null;
        String subject = exam.getMode() != null ? exam.getMode().name() : "General";
        return new StudentExamResponse(
                exam.getId(),
                exam.getTitle(),
                exam.getDescription(),
                subject,
                exam.getDurationMinutes() * 60,
                questionCount,
                "MEDIUM",
                status,
                startDate,
                enrollmentId
        );
    }
}
