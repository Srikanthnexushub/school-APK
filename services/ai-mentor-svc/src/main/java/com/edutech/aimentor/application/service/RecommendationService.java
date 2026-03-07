package com.edutech.aimentor.application.service;

import com.edutech.aimentor.application.dto.RecommendationResponse;
import com.edutech.aimentor.domain.model.PriorityLevel;
import com.edutech.aimentor.domain.model.Recommendation;
import com.edutech.aimentor.domain.model.SubjectArea;
import com.edutech.aimentor.domain.port.in.GetRecommendationsUseCase;
import com.edutech.aimentor.domain.port.out.RecommendationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class RecommendationService implements GetRecommendationsUseCase {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final RecommendationRepository recommendationRepository;

    public RecommendationService(RecommendationRepository recommendationRepository) {
        this.recommendationRepository = recommendationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations(UUID studentId) {
        return recommendationRepository.findActiveByStudentId(studentId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Creates a recommendation triggered by a weak area detected event from the performance service.
     */
    @Transactional
    public RecommendationResponse createRecommendation(UUID studentId, UUID enrollmentId,
                                                        SubjectArea subjectArea, String topic,
                                                        String recommendationText,
                                                        PriorityLevel priorityLevel) {
        Instant expiresAt = Instant.now().plus(30, ChronoUnit.DAYS);
        Recommendation recommendation = Recommendation.create(
                studentId, enrollmentId, subjectArea, topic, recommendationText, priorityLevel, expiresAt
        );
        Recommendation saved = recommendationRepository.save(recommendation);

        log.info("Recommendation created: id={} studentId={} subjectArea={} topic={}",
                saved.getId(), saved.getStudentId(), saved.getSubjectArea(), saved.getTopic());

        return toResponse(saved);
    }

    private RecommendationResponse toResponse(Recommendation rec) {
        return new RecommendationResponse(
                rec.getId(),
                rec.getStudentId(),
                rec.getEnrollmentId(),
                rec.getSubjectArea(),
                rec.getTopic(),
                rec.getRecommendationText(),
                rec.getPriorityLevel(),
                rec.isAcknowledged(),
                rec.getCreatedAt(),
                rec.getExpiresAt()
        );
    }
}
