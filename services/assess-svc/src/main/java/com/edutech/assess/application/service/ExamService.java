// src/main/java/com/edutech/assess/application/service/ExamService.java
package com.edutech.assess.application.service;

import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.CreateExamRequest;
import com.edutech.assess.application.dto.ExamResponse;
import com.edutech.assess.application.exception.AssessAccessDeniedException;
import com.edutech.assess.application.exception.ExamNotFoundException;
import com.edutech.assess.domain.event.ExamPublishedEvent;
import com.edutech.assess.domain.model.Exam;
import com.edutech.assess.domain.port.in.CreateExamUseCase;
import com.edutech.assess.domain.port.in.PublishExamUseCase;
import com.edutech.assess.domain.port.out.AssessEventPublisher;
import com.edutech.assess.domain.port.out.ExamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ExamService implements CreateExamUseCase, PublishExamUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExamService.class);

    private final ExamRepository examRepository;
    private final AssessEventPublisher eventPublisher;

    public ExamService(ExamRepository examRepository, AssessEventPublisher eventPublisher) {
        this.examRepository = examRepository;
        this.eventPublisher = eventPublisher;
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
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ExamResponse getExam(UUID examId, AuthPrincipal principal) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        if (!principal.belongsToCenter(exam.getCenterId())) {
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

    private ExamResponse toResponse(Exam e) {
        return new ExamResponse(
                e.getId(), e.getBatchId(), e.getCenterId(), e.getTitle(), e.getDescription(),
                e.getMode(), e.getDurationMinutes(), e.getMaxAttempts(), e.getStartAt(), e.getEndAt(),
                e.getTotalMarks(), e.getPassingMarks(), e.getStatus(), e.getCreatedAt()
        );
    }
}
