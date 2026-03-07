package com.edutech.examtracker.application.service;

import com.edutech.examtracker.application.dto.SubjectProgressDto;
import com.edutech.examtracker.application.dto.SyllabusModuleResponse;
import com.edutech.examtracker.application.dto.SyllabusProgressResponse;
import com.edutech.examtracker.application.dto.UpdateSyllabusModuleRequest;
import com.edutech.examtracker.application.exception.EnrollmentNotFoundException;
import com.edutech.examtracker.application.exception.ModuleNotFoundException;
import com.edutech.examtracker.domain.event.SyllabusModuleUpdatedEvent;
import com.edutech.examtracker.domain.model.ExamEnrollment;
import com.edutech.examtracker.domain.model.ModuleStatus;
import com.edutech.examtracker.domain.model.SyllabusModule;
import com.edutech.examtracker.domain.port.in.GetSyllabusProgressUseCase;
import com.edutech.examtracker.domain.port.in.UpdateSyllabusModuleUseCase;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SyllabusService implements UpdateSyllabusModuleUseCase, GetSyllabusProgressUseCase {

    private static final Logger log = LoggerFactory.getLogger(SyllabusService.class);

    private final SyllabusModuleRepository syllabusModuleRepository;
    private final ExamEnrollmentRepository enrollmentRepository;
    private final ExamTrackerEventPublisher eventPublisher;

    public SyllabusService(SyllabusModuleRepository syllabusModuleRepository,
                           ExamEnrollmentRepository enrollmentRepository,
                           ExamTrackerEventPublisher eventPublisher) {
        this.syllabusModuleRepository = syllabusModuleRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public SyllabusModuleResponse updateModule(UUID moduleId, UpdateSyllabusModuleRequest request) {
        SyllabusModule module = syllabusModuleRepository.findById(moduleId)
                .orElseThrow(() -> new ModuleNotFoundException(moduleId));

        module.updateProgress(request.completionPercent(), request.status());
        SyllabusModule saved = syllabusModuleRepository.save(module);

        eventPublisher.publish(new SyllabusModuleUpdatedEvent(
                UUID.randomUUID().toString(),
                saved.getStudentId(),
                saved.getId(),
                saved.getTopicName(),
                saved.getCompletionPercent(),
                saved.getStatus(),
                Instant.now()
        ));

        log.info("Syllabus module updated: moduleId={} status={} completionPercent={}",
                moduleId, request.status(), request.completionPercent());

        return toModuleResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SyllabusProgressResponse getSyllabusProgress(UUID studentId, UUID enrollmentId) {
        ExamEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));

        List<SyllabusModule> modules = syllabusModuleRepository.findByEnrollmentId(enrollmentId);

        Map<String, List<SyllabusModule>> bySubject = modules.stream()
                .collect(Collectors.groupingBy(SyllabusModule::getSubject));

        List<SubjectProgressDto> subjectProgress = bySubject.entrySet().stream()
                .map(entry -> {
                    String subject = entry.getKey();
                    List<SyllabusModule> subjectModules = entry.getValue();
                    int total = subjectModules.size();
                    int completed = (int) subjectModules.stream()
                            .filter(m -> m.getStatus() == ModuleStatus.COMPLETED)
                            .count();
                    BigDecimal completionPercent = total == 0 ? BigDecimal.ZERO :
                            BigDecimal.valueOf(subjectModules.stream()
                                    .mapToInt(SyllabusModule::getCompletionPercent)
                                    .average()
                                    .orElse(0.0))
                            .setScale(2, RoundingMode.HALF_UP);
                    List<SyllabusModuleResponse> topics = subjectModules.stream()
                            .map(this::toModuleResponse)
                            .toList();
                    return new SubjectProgressDto(subject, total, completed, completionPercent, topics);
                })
                .toList();

        int totalModules = modules.size();
        int completedModules = (int) modules.stream()
                .filter(m -> m.getStatus() == ModuleStatus.COMPLETED)
                .count();
        BigDecimal overallPercent = totalModules == 0 ? BigDecimal.ZERO :
                BigDecimal.valueOf(modules.stream()
                        .mapToInt(SyllabusModule::getCompletionPercent)
                        .average()
                        .orElse(0.0))
                .setScale(2, RoundingMode.HALF_UP);

        return new SyllabusProgressResponse(
                enrollmentId,
                enrollment.getExamCode(),
                subjectProgress,
                overallPercent,
                totalModules,
                completedModules
        );
    }

    private SyllabusModuleResponse toModuleResponse(SyllabusModule m) {
        return new SyllabusModuleResponse(
                m.getId(),
                m.getEnrollmentId(),
                m.getSubject(),
                m.getTopicName(),
                m.getChapterName(),
                m.getWeightagePercent(),
                m.getStatus(),
                m.getCompletionPercent(),
                m.getLastStudiedAt(),
                m.getCreatedAt()
        );
    }
}
