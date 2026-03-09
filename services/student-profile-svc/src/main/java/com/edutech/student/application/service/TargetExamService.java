package com.edutech.student.application.service;

import com.edutech.student.application.dto.SetTargetExamRequest;
import com.edutech.student.application.dto.TargetExamResponse;
import com.edutech.student.application.exception.StudentNotFoundException;
import com.edutech.student.domain.event.TargetExamSetEvent;
import com.edutech.student.domain.model.TargetExam;
import com.edutech.student.domain.port.in.SetTargetExamUseCase;
import com.edutech.student.domain.port.out.StudentEventPublisher;
import com.edutech.student.domain.port.out.StudentProfileRepository;
import com.edutech.student.domain.port.out.TargetExamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TargetExamService implements SetTargetExamUseCase {

    private static final Logger log = LoggerFactory.getLogger(TargetExamService.class);

    private final StudentProfileRepository profileRepository;
    private final TargetExamRepository targetExamRepository;
    private final StudentEventPublisher eventPublisher;

    public TargetExamService(StudentProfileRepository profileRepository,
                              TargetExamRepository targetExamRepository,
                              StudentEventPublisher eventPublisher) {
        this.profileRepository = profileRepository;
        this.targetExamRepository = targetExamRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public TargetExamResponse setTargetExam(UUID studentId, SetTargetExamRequest request) {
        profileRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));

        // Soft-delete any existing active target exam for the same examCode
        targetExamRepository.findByStudentId(studentId).stream()
                .filter(e -> e.getExamCode() == request.examCode() && e.getDeletedAt() == null)
                .forEach(e -> {
                    e.softDelete();
                    targetExamRepository.save(e);
                });

        TargetExam exam = TargetExam.create(
                studentId,
                request.examCode(),
                request.targetYear(),
                request.priority()
        );

        TargetExam saved = targetExamRepository.save(exam);
        log.info("Target exam set: id={} studentId={} examCode={}", saved.getId(), studentId, saved.getExamCode());

        eventPublisher.publish(new TargetExamSetEvent(
                UUID.randomUUID().toString(),
                studentId,
                saved.getExamCode(),
                saved.getTargetYear(),
                Instant.now()
        ));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TargetExamResponse> getExamsByStudentId(UUID studentId) {
        profileRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));
        return targetExamRepository.findByStudentId(studentId).stream()
                .map(this::toResponse)
                .toList();
    }

    public void softDeleteExam(UUID studentId, UUID examId) {
        profileRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));
        targetExamRepository.deleteById(examId);
    }

    private TargetExamResponse toResponse(TargetExam e) {
        return new TargetExamResponse(
                e.getId(),
                e.getStudentId(),
                e.getExamCode(),
                e.getTargetYear(),
                e.getPriority(),
                e.getCreatedAt()
        );
    }
}
