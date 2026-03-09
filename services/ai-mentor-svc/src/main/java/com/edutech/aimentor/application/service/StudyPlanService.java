package com.edutech.aimentor.application.service;

import com.edutech.aimentor.application.dto.CreateStudyPlanRequest;
import com.edutech.aimentor.application.dto.StudyPlanItemResponse;
import com.edutech.aimentor.application.dto.StudyPlanResponse;
import com.edutech.aimentor.application.exception.StudyPlanNotFoundException;
import com.edutech.aimentor.domain.event.StudyPlanCreatedEvent;
import com.edutech.aimentor.domain.model.PriorityLevel;
import com.edutech.aimentor.domain.model.StudyPlan;
import com.edutech.aimentor.domain.model.StudyPlanItem;
import com.edutech.aimentor.domain.port.in.CreateStudyPlanUseCase;
import com.edutech.aimentor.domain.port.in.GetStudyPlanUseCase;
import com.edutech.aimentor.domain.port.in.ListStudyPlansUseCase;
import com.edutech.aimentor.domain.port.in.UpdateStudyPlanItemUseCase;
import com.edutech.aimentor.domain.port.out.AiMentorEventPublisher;
import com.edutech.aimentor.domain.port.out.StudyPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class StudyPlanService implements CreateStudyPlanUseCase, GetStudyPlanUseCase, ListStudyPlansUseCase, UpdateStudyPlanItemUseCase {

    private static final Logger log = LoggerFactory.getLogger(StudyPlanService.class);

    private final StudyPlanRepository studyPlanRepository;
    private final AiMentorEventPublisher eventPublisher;

    public StudyPlanService(StudyPlanRepository studyPlanRepository,
                            AiMentorEventPublisher eventPublisher) {
        this.studyPlanRepository = studyPlanRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public StudyPlanResponse createStudyPlan(CreateStudyPlanRequest request) {
        StudyPlan studyPlan = StudyPlan.create(
                request.studentId(),
                request.enrollmentId(),
                request.title(),
                request.description(),
                request.targetExamDate()
        );

        if (request.items() != null) {
            for (CreateStudyPlanRequest.StudyPlanItemRequest itemRequest : request.items()) {
                PriorityLevel priority = itemRequest.priorityLevel() != null
                        ? itemRequest.priorityLevel()
                        : PriorityLevel.MEDIUM;
                StudyPlanItem item = StudyPlanItem.create(
                        studyPlan,
                        itemRequest.subjectArea(),
                        itemRequest.topic(),
                        priority
                );
                studyPlan.addItem(item);
            }
        }

        StudyPlan saved = studyPlanRepository.save(studyPlan);

        try {
            eventPublisher.publishStudyPlanCreated(new StudyPlanCreatedEvent(
                    saved.getId(),
                    saved.getStudentId(),
                    saved.getEnrollmentId(),
                    saved.getTitle(),
                    Instant.now()
            ));
        } catch (Exception e) {
            log.warn("Failed to publish StudyPlanCreatedEvent for studyPlanId={}: {}",
                    saved.getId(), e.getMessage(), e);
        }

        log.info("Study plan created: id={} studentId={} enrollmentId={}",
                saved.getId(), saved.getStudentId(), saved.getEnrollmentId());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StudyPlanResponse getStudyPlan(UUID studentId, UUID enrollmentId) {
        StudyPlan studyPlan = studyPlanRepository
                .findByStudentIdAndEnrollmentId(studentId, enrollmentId)
                .orElseThrow(() -> new StudyPlanNotFoundException(studentId, enrollmentId));
        return toResponse(studyPlan);
    }

    @Override
    @Transactional(readOnly = true)
    public StudyPlanResponse getStudyPlanById(UUID planId, UUID studentId) {
        StudyPlan studyPlan = studyPlanRepository.findById(planId)
                .orElseThrow(() -> new StudyPlanNotFoundException(planId));
        return toResponse(studyPlan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudyPlanResponse> listStudyPlans(UUID studentId) {
        return studyPlanRepository.findAllByStudentId(studentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public StudyPlanItemResponse reviewItem(UUID itemId, UUID studentId, int quality) {
        StudyPlanItem item = studyPlanRepository.findItemById(itemId)
                .orElseThrow(() -> new StudyPlanNotFoundException(itemId));

        item.applyReview(quality);
        StudyPlanItem saved = studyPlanRepository.saveItem(item);

        log.info("Study plan item reviewed: itemId={} studentId={} quality={} nextReviewAt={}",
                saved.getId(), studentId, quality, saved.getNextReviewAt());

        return toItemResponse(saved);
    }

    private StudyPlanResponse toResponse(StudyPlan plan) {
        List<StudyPlanItemResponse> itemResponses = plan.getItems().stream()
                .filter(item -> item.getDeletedAt() == null)
                .map(this::toItemResponse)
                .toList();

        return new StudyPlanResponse(
                plan.getId(),
                plan.getStudentId(),
                plan.getEnrollmentId(),
                plan.getTitle(),
                plan.getDescription(),
                plan.getTargetExamDate(),
                plan.isActive(),
                itemResponses,
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }

    private StudyPlanItemResponse toItemResponse(StudyPlanItem item) {
        return new StudyPlanItemResponse(
                item.getId(),
                item.getSubjectArea(),
                item.getTopic(),
                item.getPriorityLevel(),
                item.getInterval(),
                item.getRepetitions(),
                item.getEaseFactor(),
                item.getNextReviewAt(),
                item.getLastReviewedAt(),
                item.getQuality(),
                item.getCreatedAt()
        );
    }
}
