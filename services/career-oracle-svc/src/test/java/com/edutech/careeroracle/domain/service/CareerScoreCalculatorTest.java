package com.edutech.careeroracle.domain.service;

import com.edutech.careeroracle.domain.model.CareerStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CareerScoreCalculatorTest {

    private CareerScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CareerScoreCalculator();
    }

    @Test
    void calculate_engineeringStream_highScore() {
        // PCM subjects all 90% → ENGINEERING should get highest fit score
        BigDecimal ersScore = BigDecimal.valueOf(85);
        String academicStream = "SCIENCE";
        Map<String, BigDecimal> subjectStrengths = Map.of(
                "PHYSICS", BigDecimal.valueOf(90),
                "CHEMISTRY", BigDecimal.valueOf(90),
                "MATHS", BigDecimal.valueOf(90)
        );

        Map<CareerStream, BigDecimal> scores = calculator.calculate(ersScore, academicStream, subjectStrengths);

        assertThat(scores).containsKey(CareerStream.ENGINEERING);
        BigDecimal engineeringScore = scores.get(CareerStream.ENGINEERING);
        assertThat(engineeringScore).isGreaterThan(BigDecimal.valueOf(70));

        // ENGINEERING should score higher than MEDICAL (no bio scores)
        assertThat(engineeringScore).isGreaterThan(scores.get(CareerStream.MEDICAL));
    }

    @Test
    void calculate_medicalStream_highScore() {
        // PCB subjects all 90% → MEDICAL should get highest fit score among bio-related streams
        BigDecimal ersScore = BigDecimal.valueOf(85);
        String academicStream = "SCIENCE";
        Map<String, BigDecimal> subjectStrengths = Map.of(
                "BIOLOGY", BigDecimal.valueOf(90),
                "CHEMISTRY", BigDecimal.valueOf(90),
                "PHYSICS", BigDecimal.valueOf(50)
        );

        Map<CareerStream, BigDecimal> scores = calculator.calculate(ersScore, academicStream, subjectStrengths);

        assertThat(scores).containsKey(CareerStream.MEDICAL);
        BigDecimal medicalScore = scores.get(CareerStream.MEDICAL);
        assertThat(medicalScore).isGreaterThan(BigDecimal.valueOf(70));

        // MEDICAL should score higher than ENGINEERING (weak physics vs strong bio)
        BigDecimal engineeringScore = scores.get(CareerStream.ENGINEERING);
        assertThat(medicalScore).isGreaterThan(engineeringScore);
    }

    @Test
    void calculate_commerceStream() {
        // Maths+Economics 85% → COMMERCE should get high fit score
        BigDecimal ersScore = BigDecimal.valueOf(80);
        String academicStream = "COMMERCE";
        Map<String, BigDecimal> subjectStrengths = Map.of(
                "MATHS", BigDecimal.valueOf(85),
                "ECONOMICS", BigDecimal.valueOf(85)
        );

        Map<CareerStream, BigDecimal> scores = calculator.calculate(ersScore, academicStream, subjectStrengths);

        assertThat(scores).containsKey(CareerStream.COMMERCE);
        BigDecimal commerceScore = scores.get(CareerStream.COMMERCE);
        assertThat(commerceScore).isGreaterThan(BigDecimal.valueOf(60));
    }

    @Test
    void calculate_lowErsScore_allStreamsLow() {
        // ERS=20 → all fit scores should be < 50
        BigDecimal ersScore = BigDecimal.valueOf(20);
        String academicStream = "SCIENCE";
        Map<String, BigDecimal> subjectStrengths = Map.of(
                "PHYSICS", BigDecimal.valueOf(20),
                "CHEMISTRY", BigDecimal.valueOf(20),
                "MATHS", BigDecimal.valueOf(20)
        );

        Map<CareerStream, BigDecimal> scores = calculator.calculate(ersScore, academicStream, subjectStrengths);

        assertThat(scores).isNotEmpty();
        for (Map.Entry<CareerStream, BigDecimal> entry : scores.entrySet()) {
            assertThat(entry.getValue())
                    .as("Score for %s should be < 50 when ERS is low", entry.getKey())
                    .isLessThan(BigDecimal.valueOf(50));
        }
    }

    @Test
    void calculate_returnsAllStreams() {
        // Result map should contain all CareerStream enum values
        BigDecimal ersScore = BigDecimal.valueOf(60);
        String academicStream = "SCIENCE";
        Map<String, BigDecimal> subjectStrengths = Map.of(
                "PHYSICS", BigDecimal.valueOf(70),
                "CHEMISTRY", BigDecimal.valueOf(70),
                "MATHS", BigDecimal.valueOf(70)
        );

        Map<CareerStream, BigDecimal> scores = calculator.calculate(ersScore, academicStream, subjectStrengths);

        for (CareerStream stream : CareerStream.values()) {
            assertThat(scores).as("Result should contain all CareerStream values").containsKey(stream);
            assertThat(scores.get(stream))
                    .as("Score for %s should be between 0 and 100", stream)
                    .isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
        }
    }
}
