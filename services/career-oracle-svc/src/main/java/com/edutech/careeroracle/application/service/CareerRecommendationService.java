package com.edutech.careeroracle.application.service;

import com.edutech.careeroracle.application.dto.CareerRecommendationResponse;
import com.edutech.careeroracle.application.exception.CareerProfileNotFoundException;
import com.edutech.careeroracle.domain.event.CareerRecommendedEvent;
import com.edutech.careeroracle.domain.model.CareerProfile;
import com.edutech.careeroracle.domain.model.CareerRecommendation;
import com.edutech.careeroracle.domain.model.CareerStream;
import com.edutech.careeroracle.domain.model.ConfidenceLevel;
import com.edutech.careeroracle.domain.port.in.GenerateCareerRecommendationsUseCase;
import com.edutech.careeroracle.domain.port.out.CareerOracleEventPublisher;
import com.edutech.careeroracle.domain.port.out.CareerProfileRepository;
import com.edutech.careeroracle.domain.port.out.CareerRecommendationRepository;
import com.edutech.careeroracle.domain.service.CareerScoreCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CareerRecommendationService implements GenerateCareerRecommendationsUseCase {

    private static final Logger log = LoggerFactory.getLogger(CareerRecommendationService.class);

    private final CareerProfileRepository careerProfileRepository;
    private final CareerRecommendationRepository careerRecommendationRepository;
    private final CareerOracleEventPublisher eventPublisher;
    private final CareerScoreCalculator careerScoreCalculator;

    public CareerRecommendationService(CareerProfileRepository careerProfileRepository,
                                        CareerRecommendationRepository careerRecommendationRepository,
                                        CareerOracleEventPublisher eventPublisher,
                                        CareerScoreCalculator careerScoreCalculator) {
        this.careerProfileRepository = careerProfileRepository;
        this.careerRecommendationRepository = careerRecommendationRepository;
        this.eventPublisher = eventPublisher;
        this.careerScoreCalculator = careerScoreCalculator;
    }

    @Override
    @Transactional
    public List<CareerRecommendationResponse> generateRecommendations(UUID studentId,
                                                                       Map<String, BigDecimal> subjectStrengths) {
        CareerProfile profile = careerProfileRepository.findByStudentId(studentId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new CareerProfileNotFoundException("studentId", studentId));

        Map<CareerStream, BigDecimal> fitScores = careerScoreCalculator.calculate(
                profile.getErsScore(),
                profile.getAcademicStream(),
                subjectStrengths
        );

        careerRecommendationRepository.deactivateAllByStudentId(studentId);

        List<Map.Entry<CareerStream, BigDecimal>> sortedEntries = fitScores.entrySet().stream()
                .sorted(Map.Entry.<CareerStream, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());

        OffsetDateTime validUntil = OffsetDateTime.now().plusMonths(3);
        List<CareerRecommendation> recommendations = new ArrayList<>();

        for (int i = 0; i < sortedEntries.size(); i++) {
            Map.Entry<CareerStream, BigDecimal> entry = sortedEntries.get(i);
            CareerStream stream = entry.getKey();
            BigDecimal score = entry.getValue();

            ConfidenceLevel confidence = determineConfidenceLevel(score);
            String rationale = buildRationale(stream, score, profile.getAcademicStream());

            CareerRecommendation recommendation = CareerRecommendation.create(
                    studentId,
                    stream,
                    score,
                    confidence,
                    rationale,
                    i + 1,
                    validUntil
            );
            recommendations.add(recommendation);
        }

        List<CareerRecommendation> saved = careerRecommendationRepository.saveAll(recommendations);

        List<CareerStream> recommendedStreams = saved.stream()
                .map(CareerRecommendation::getCareerStream)
                .collect(Collectors.toList());

        CareerStream topStream = saved.isEmpty() ? null : saved.get(0).getCareerStream();

        CareerRecommendedEvent event = new CareerRecommendedEvent(
                studentId,
                recommendedStreams,
                topStream,
                OffsetDateTime.now()
        );

        try {
            eventPublisher.publishCareerRecommended(event);
        } catch (Exception ex) {
            log.warn("Failed to publish CareerRecommendedEvent for studentId={}: {}", studentId, ex.getMessage());
        }

        return saved.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CareerRecommendationResponse> getActiveRecommendations(UUID studentId) {
        return careerRecommendationRepository.findActiveByStudentId(studentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ConfidenceLevel determineConfidenceLevel(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return ConfidenceLevel.VERY_HIGH;
        } else if (score.compareTo(BigDecimal.valueOf(55)) >= 0) {
            return ConfidenceLevel.HIGH;
        } else if (score.compareTo(BigDecimal.valueOf(35)) >= 0) {
            return ConfidenceLevel.MODERATE;
        } else {
            return ConfidenceLevel.LOW;
        }
    }

    private String buildRationale(CareerStream stream, BigDecimal score, String academicStream) {
        return String.format("Based on your %s academic background and overall performance, "
                        + "%s shows a fit score of %.2f. "
                        + "This recommendation is computed from your ERS score and subject strengths.",
                academicStream, stream.name(), score);
    }

    private CareerRecommendationResponse toResponse(CareerRecommendation recommendation) {
        return new CareerRecommendationResponse(
                recommendation.getId(),
                recommendation.getStudentId(),
                recommendation.getCareerStream(),
                recommendation.getFitScore(),
                recommendation.getConfidenceLevel(),
                recommendation.getRationale(),
                recommendation.getRankOrder(),
                recommendation.getGeneratedAt(),
                recommendation.getValidUntil(),
                recommendation.getIsActive()
        );
    }
}
