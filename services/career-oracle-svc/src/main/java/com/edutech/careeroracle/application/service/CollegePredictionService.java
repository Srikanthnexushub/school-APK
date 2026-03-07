package com.edutech.careeroracle.application.service;

import com.edutech.careeroracle.application.dto.CollegePredictionResponse;
import com.edutech.careeroracle.application.exception.CareerProfileNotFoundException;
import com.edutech.careeroracle.domain.model.CareerProfile;
import com.edutech.careeroracle.domain.model.CollegePrediction;
import com.edutech.careeroracle.domain.model.CollegeTier;
import com.edutech.careeroracle.domain.port.in.PredictCollegesUseCase;
import com.edutech.careeroracle.domain.port.out.CareerProfileRepository;
import com.edutech.careeroracle.domain.port.out.CollegePredictionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CollegePredictionService implements PredictCollegesUseCase {

    private final CareerProfileRepository careerProfileRepository;
    private final CollegePredictionRepository collegePredictionRepository;

    public CollegePredictionService(CareerProfileRepository careerProfileRepository,
                                     CollegePredictionRepository collegePredictionRepository) {
        this.careerProfileRepository = careerProfileRepository;
        this.collegePredictionRepository = collegePredictionRepository;
    }

    @Override
    @Transactional
    public List<CollegePredictionResponse> predictColleges(UUID studentId) {
        CareerProfile profile = careerProfileRepository.findByStudentId(studentId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new CareerProfileNotFoundException("studentId", studentId));

        BigDecimal ersScore = profile.getErsScore() != null ? profile.getErsScore() : BigDecimal.ZERO;

        List<CollegePrediction> predictions = generatePredictions(profile, ersScore);
        List<CollegePrediction> saved = collegePredictionRepository.saveAll(predictions);

        return saved.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollegePredictionResponse> getPredictions(UUID studentId) {
        return collegePredictionRepository.findByStudentId(studentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private List<CollegePrediction> generatePredictions(CareerProfile profile, BigDecimal ersScore) {
        List<CollegePrediction> predictions = new ArrayList<>();

        String preferredStream = profile.getPreferredCareerStream() != null
                ? profile.getPreferredCareerStream().name()
                : "ENGINEERING";

        // Tier 1 prediction
        BigDecimal tier1Cutoff = BigDecimal.valueOf(90);
        BigDecimal tier1Probability = calculateProbability(ersScore, tier1Cutoff);
        predictions.add(CollegePrediction.create(
                profile.getStudentId(),
                profile.getEnrollmentId(),
                resolveTierCollegeName(CollegeTier.TIER_1, preferredStream),
                resolveCourse(preferredStream),
                CollegeTier.TIER_1,
                tier1Cutoff,
                ersScore,
                tier1Probability
        ));

        // Tier 2 prediction
        BigDecimal tier2Cutoff = BigDecimal.valueOf(75);
        BigDecimal tier2Probability = calculateProbability(ersScore, tier2Cutoff);
        predictions.add(CollegePrediction.create(
                profile.getStudentId(),
                profile.getEnrollmentId(),
                resolveTierCollegeName(CollegeTier.TIER_2, preferredStream),
                resolveCourse(preferredStream),
                CollegeTier.TIER_2,
                tier2Cutoff,
                ersScore,
                tier2Probability
        ));

        // Tier 3 prediction
        BigDecimal tier3Cutoff = BigDecimal.valueOf(55);
        BigDecimal tier3Probability = calculateProbability(ersScore, tier3Cutoff);
        predictions.add(CollegePrediction.create(
                profile.getStudentId(),
                profile.getEnrollmentId(),
                resolveTierCollegeName(CollegeTier.TIER_3, preferredStream),
                resolveCourse(preferredStream),
                CollegeTier.TIER_3,
                tier3Cutoff,
                ersScore,
                tier3Probability
        ));

        return predictions;
    }

    private BigDecimal calculateProbability(BigDecimal ersScore, BigDecimal cutoff) {
        if (ersScore.compareTo(cutoff) >= 0) {
            BigDecimal excess = ersScore.subtract(cutoff);
            BigDecimal probability = BigDecimal.valueOf(70).add(excess.multiply(BigDecimal.valueOf(0.3)));
            return probability.min(BigDecimal.valueOf(99)).setScale(2, RoundingMode.HALF_UP);
        } else {
            BigDecimal deficit = cutoff.subtract(ersScore);
            BigDecimal probability = BigDecimal.valueOf(70).subtract(deficit.multiply(BigDecimal.valueOf(0.7)));
            return probability.max(BigDecimal.valueOf(5)).setScale(2, RoundingMode.HALF_UP);
        }
    }

    private String resolveTierCollegeName(CollegeTier tier, String stream) {
        return switch (tier) {
            case TIER_1 -> "Premier Institute of " + capitalize(stream);
            case TIER_2 -> "State University of " + capitalize(stream);
            case TIER_3 -> "Regional College of " + capitalize(stream);
        };
    }

    private String resolveCourse(String stream) {
        return switch (stream) {
            case "ENGINEERING" -> "B.Tech";
            case "MEDICAL" -> "MBBS";
            case "COMMERCE", "MANAGEMENT" -> "BBA";
            case "ARTS", "LAW" -> "BA LLB";
            case "CIVIL_SERVICES" -> "B.A. Political Science";
            case "RESEARCH" -> "B.Sc. Research";
            default -> "B.Sc.";
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private CollegePredictionResponse toResponse(CollegePrediction prediction) {
        return new CollegePredictionResponse(
                prediction.getId(),
                prediction.getStudentId(),
                prediction.getEnrollmentId(),
                prediction.getCollegeName(),
                prediction.getCourseName(),
                prediction.getCollegeTier(),
                prediction.getPredictedCutoff(),
                prediction.getStudentPredictedScore(),
                prediction.getAdmissionProbability(),
                prediction.getGeneratedAt()
        );
    }
}
