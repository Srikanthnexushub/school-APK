package com.edutech.examtracker.application.service;

import com.edutech.examtracker.application.dto.EnrollInExamRequest;
import com.edutech.examtracker.application.dto.ExamEnrollmentResponse;
import com.edutech.examtracker.application.exception.DuplicateEnrollmentException;
import com.edutech.examtracker.application.exception.EnrollmentNotFoundException;
import com.edutech.examtracker.domain.event.ExamEnrolledEvent;
import com.edutech.examtracker.domain.model.ExamEnrollment;
import com.edutech.examtracker.domain.model.ModuleStatus;
import com.edutech.examtracker.domain.model.SyllabusModule;
import com.edutech.examtracker.domain.port.in.EnrollInExamUseCase;
import com.edutech.examtracker.domain.port.in.GetEnrollmentUseCase;
import com.edutech.examtracker.domain.port.out.ExamEnrollmentRepository;
import com.edutech.examtracker.domain.port.out.ExamTrackerEventPublisher;
import com.edutech.examtracker.domain.port.out.SyllabusModuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ExamEnrollmentService implements EnrollInExamUseCase, GetEnrollmentUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExamEnrollmentService.class);

    private final ExamEnrollmentRepository enrollmentRepository;
    private final SyllabusModuleRepository syllabusModuleRepository;
    private final ExamTrackerEventPublisher eventPublisher;

    public ExamEnrollmentService(ExamEnrollmentRepository enrollmentRepository,
                                 SyllabusModuleRepository syllabusModuleRepository,
                                 ExamTrackerEventPublisher eventPublisher) {
        this.enrollmentRepository = enrollmentRepository;
        this.syllabusModuleRepository = syllabusModuleRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ExamEnrollmentResponse enroll(UUID studentId, EnrollInExamRequest request) {
        enrollmentRepository.findByStudentIdAndExamCode(studentId, request.examCode())
                .ifPresent(existing -> {
                    throw new DuplicateEnrollmentException(studentId, request.examCode());
                });

        ExamEnrollment enrollment = request.examDate() != null
                ? ExamEnrollment.create(studentId, request.examCode(), request.examName(),
                        request.targetYear(), request.examDate())
                : ExamEnrollment.create(studentId, request.examCode(), request.examName(),
                        request.targetYear());

        ExamEnrollment saved = enrollmentRepository.save(enrollment);

        eventPublisher.publish(new ExamEnrolledEvent(
                UUID.randomUUID().toString(),
                studentId,
                saved.getId(),
                saved.getExamCode(),
                saved.getTargetYear(),
                Instant.now()
        ));

        log.info("Student enrolled: studentId={} examCode={} enrollmentId={}",
                studentId, request.examCode(), saved.getId());

        return toResponse(saved, 0, 0, BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamEnrollmentResponse getEnrollment(UUID enrollmentId) {
        ExamEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));

        List<SyllabusModule> modules = syllabusModuleRepository.findByEnrollmentId(enrollmentId);
        int total = modules.size();
        int completed = (int) modules.stream()
                .filter(m -> m.getStatus() == ModuleStatus.COMPLETED)
                .count();
        BigDecimal overallPercent = calculateOverallPercent(modules);

        return toResponse(enrollment, total, completed, overallPercent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamEnrollmentResponse> getStudentEnrollments(UUID studentId) {
        return enrollmentRepository.findByStudentId(studentId).stream()
                .sorted(Comparator.comparing(ExamEnrollment::getCreatedAt).reversed())
                .map(e -> {
                    List<SyllabusModule> modules = syllabusModuleRepository.findByEnrollmentId(e.getId());
                    int total = modules.size();
                    int completed = (int) modules.stream()
                            .filter(m -> m.getStatus() == ModuleStatus.COMPLETED)
                            .count();
                    BigDecimal overallPercent = calculateOverallPercent(modules);
                    return toResponse(e, total, completed, overallPercent);
                })
                .toList();
    }

    private BigDecimal calculateOverallPercent(List<SyllabusModule> modules) {
        if (modules.isEmpty()) {
            return BigDecimal.ZERO;
        }
        double avg = modules.stream()
                .mapToInt(SyllabusModule::getCompletionPercent)
                .average()
                .orElse(0.0);
        return BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
    }

    private ExamEnrollmentResponse toResponse(ExamEnrollment e, int total, int completed,
                                               BigDecimal overallPercent) {
        return new ExamEnrollmentResponse(
                e.getId(),
                e.getStudentId(),
                e.getExamCode(),
                e.getExamName(),
                e.getExamDate(),
                e.getTargetYear(),
                e.getStatus(),
                total,
                completed,
                overallPercent,
                e.getCreatedAt()
        );
    }
}
