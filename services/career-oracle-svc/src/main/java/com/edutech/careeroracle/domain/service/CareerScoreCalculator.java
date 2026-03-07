package com.edutech.careeroracle.domain.service;

import com.edutech.careeroracle.domain.model.CareerStream;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

/**
 * Pure domain service — no Spring annotations.
 * Calculates a careerFitScore (BigDecimal 0-100) for each CareerStream
 * based on ERS score, academic stream, and subject strengths.
 */
public class CareerScoreCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal BASE_MULTIPLIER = new BigDecimal("0.6");
    private static final BigDecimal SUBJECT_WEIGHT = new BigDecimal("0.4");

    /**
     * Calculates career fit scores for all CareerStream values.
     *
     * @param ersScore         ERS score from performance-svc (0-100)
     * @param academicStream   student's academic stream: SCIENCE, COMMERCE, ARTS
     * @param subjectStrengths map of subject name (uppercase) to mastery percent (0-100)
     * @return map of CareerStream to fit score (0-100)
     */
    public Map<CareerStream, BigDecimal> calculate(BigDecimal ersScore,
                                                    String academicStream,
                                                    Map<String, BigDecimal> subjectStrengths) {
        if (ersScore == null) {
            ersScore = BigDecimal.ZERO;
        }
        if (academicStream == null) {
            academicStream = "";
        }
        if (subjectStrengths == null) {
            subjectStrengths = Map.of();
        }

        Map<CareerStream, BigDecimal> scores = new EnumMap<>(CareerStream.class);
        String stream = academicStream.toUpperCase();

        BigDecimal baseScore = ersScore.multiply(BASE_MULTIPLIER);

        for (CareerStream careerStream : CareerStream.values()) {
            BigDecimal fitScore = computeFitScore(careerStream, ersScore, baseScore, stream, subjectStrengths);
            scores.put(careerStream, fitScore.min(HUNDRED).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        }

        return scores;
    }

    private BigDecimal computeFitScore(CareerStream careerStream, BigDecimal ersScore,
                                        BigDecimal baseScore, String academicStream,
                                        Map<String, BigDecimal> subjectStrengths) {
        switch (careerStream) {
            case ENGINEERING -> {
                if ("SCIENCE".equals(academicStream)) {
                    BigDecimal physics = subjectStrengths.getOrDefault("PHYSICS", BigDecimal.ZERO);
                    BigDecimal chemistry = subjectStrengths.getOrDefault("CHEMISTRY", BigDecimal.ZERO);
                    BigDecimal maths = subjectStrengths.getOrDefault("MATHS", subjectStrengths.getOrDefault("MATHEMATICS", BigDecimal.ZERO));
                    BigDecimal subjectAvg = physics.add(chemistry).add(maths).divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP);
                    return baseScore.add(subjectAvg.multiply(SUBJECT_WEIGHT));
                }
                return baseScore.multiply(new BigDecimal("0.5"));
            }
            case MEDICAL -> {
                if ("SCIENCE".equals(academicStream)) {
                    BigDecimal biology = subjectStrengths.getOrDefault("BIOLOGY", BigDecimal.ZERO);
                    BigDecimal chemistry = subjectStrengths.getOrDefault("CHEMISTRY", BigDecimal.ZERO);
                    BigDecimal subjectAvg = biology.add(chemistry).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
                    return baseScore.add(subjectAvg.multiply(SUBJECT_WEIGHT));
                }
                return baseScore.multiply(new BigDecimal("0.5"));
            }
            case COMMERCE -> {
                if ("COMMERCE".equals(academicStream) || "SCIENCE".equals(academicStream)) {
                    BigDecimal maths = subjectStrengths.getOrDefault("MATHS", subjectStrengths.getOrDefault("MATHEMATICS", BigDecimal.ZERO));
                    BigDecimal economics = subjectStrengths.getOrDefault("ECONOMICS", BigDecimal.ZERO);
                    BigDecimal subjectAvg = maths.add(economics).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
                    return baseScore.add(subjectAvg.multiply(SUBJECT_WEIGHT));
                }
                return baseScore.multiply(new BigDecimal("0.7"));
            }
            case ARTS -> {
                if ("ARTS".equals(academicStream)) {
                    return ersScore.multiply(new BigDecimal("0.75"));
                }
                return baseScore;
            }
            case LAW -> {
                BigDecimal streamBonus = "ARTS".equals(academicStream) ? new BigDecimal("0.15") : new BigDecimal("0.05");
                return ersScore.multiply(new BigDecimal("0.6").add(streamBonus));
            }
            case CIVIL_SERVICES -> {
                return ersScore.multiply(new BigDecimal("0.65"));
            }
            case MANAGEMENT -> {
                if ("COMMERCE".equals(academicStream)) {
                    return ersScore.multiply(new BigDecimal("0.75"));
                }
                return ersScore.multiply(new BigDecimal("0.65"));
            }
            case RESEARCH -> {
                if ("SCIENCE".equals(academicStream)) {
                    return ersScore.multiply(new BigDecimal("0.7"));
                }
                return baseScore;
            }
            default -> {
                return baseScore;
            }
        }
    }
}
