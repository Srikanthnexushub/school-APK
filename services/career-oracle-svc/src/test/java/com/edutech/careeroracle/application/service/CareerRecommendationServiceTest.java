package com.edutech.careeroracle.application.service;

import com.edutech.careeroracle.application.dto.CareerRecommendationResponse;
import com.edutech.careeroracle.application.exception.CareerProfileNotFoundException;
import com.edutech.careeroracle.domain.model.CareerProfile;
import com.edutech.careeroracle.domain.model.CareerRecommendation;
import com.edutech.careeroracle.domain.model.CareerStream;
import com.edutech.careeroracle.domain.model.ConfidenceLevel;
import com.edutech.careeroracle.domain.port.out.CareerOracleEventPublisher;
import com.edutech.careeroracle.domain.port.out.CareerProfileRepository;
import com.edutech.careeroracle.domain.port.out.CareerRecommendationRepository;
import com.edutech.careeroracle.domain.service.CareerScoreCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CareerRecommendationServiceTest {

    @Mock
    private CareerProfileRepository careerProfileRepository;

    @Mock
    private CareerRecommendationRepository careerRecommendationRepository;

    @Mock
    private CareerOracleEventPublisher eventPublisher;

    private CareerScoreCalculator careerScoreCalculator;
    private CareerRecommendationService service;

    private UUID studentId;
    private CareerProfile testProfile;

    @BeforeEach
    void setUp() {
        careerScoreCalculator = new CareerScoreCalculator();
        service = new CareerRecommendationService(
                careerProfileRepository,
                careerRecommendationRepository,
                eventPublisher,
                careerScoreCalculator
        );

        studentId = UUID.randomUUID();
        testProfile = CareerProfile.create(
                studentId,
                UUID.randomUUID(),
                "SCIENCE",
                11,
                BigDecimal.valueOf(78),
                CareerStream.ENGINEERING
        );

        doNothing().when(careerRecommendationRepository).deactivateAllByStudentId(any());
        doNothing().when(eventPublisher).publishCareerRecommended(any());
    }

    @Test
    void generateRecommendations_success() {
        when(careerProfileRepository.findByStudentId(studentId)).thenReturn(Optional.of(testProfile));
        when(careerRecommendationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, BigDecimal> subjectStrengths = Map.of(
                "PHYSICS", BigDecimal.valueOf(80),
                "CHEMISTRY", BigDecimal.valueOf(75),
                "MATHS", BigDecimal.valueOf(85)
        );

        List<CareerRecommendationResponse> result = service.generateRecommendations(studentId, subjectStrengths);

        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(CareerStream.values().length);
        verify(careerRecommendationRepository).deactivateAllByStudentId(studentId);
        verify(careerRecommendationRepository).saveAll(anyList());
    }

    @Test
    void generateRecommendations_rankedByFitScore() {
        when(careerProfileRepository.findByStudentId(studentId)).thenReturn(Optional.of(testProfile));
        when(careerRecommendationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, BigDecimal> subjectStrengths = Map.of(
                "PHYSICS", BigDecimal.valueOf(90),
                "CHEMISTRY", BigDecimal.valueOf(90),
                "MATHS", BigDecimal.valueOf(90)
        );

        List<CareerRecommendationResponse> result = service.generateRecommendations(studentId, subjectStrengths);

        assertThat(result).isNotEmpty();

        // Verify recommendations are ranked by fit score descending (rank 1 has highest score)
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).fitScore())
                    .as("Recommendation at rank %d should have higher or equal fit score than rank %d",
                            result.get(i).rankOrder(), result.get(i + 1).rankOrder())
                    .isGreaterThanOrEqualTo(result.get(i + 1).fitScore());
        }

        // First recommendation should have rank 1
        assertThat(result.get(0).rankOrder()).isEqualTo(1);
    }

    @Test
    void generateRecommendations_publishesEvent() {
        when(careerProfileRepository.findByStudentId(studentId)).thenReturn(Optional.of(testProfile));
        when(careerRecommendationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        service.generateRecommendations(studentId, Map.of());

        verify(eventPublisher).publishCareerRecommended(any());
    }

    @Test
    void generateRecommendations_profileNotFound() {
        UUID unknownStudentId = UUID.randomUUID();
        when(careerProfileRepository.findByStudentId(unknownStudentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateRecommendations(unknownStudentId, Map.of()))
                .isInstanceOf(CareerProfileNotFoundException.class)
                .hasMessageContaining(unknownStudentId.toString());
    }

    @Test
    void getRecommendations_returnsLatestActive() {
        CareerRecommendation active1 = CareerRecommendation.create(
                studentId, CareerStream.ENGINEERING, BigDecimal.valueOf(85),
                ConfidenceLevel.VERY_HIGH, "Top engineering fit", 1,
                OffsetDateTime.now().plusMonths(3)
        );
        CareerRecommendation active2 = CareerRecommendation.create(
                studentId, CareerStream.RESEARCH, BigDecimal.valueOf(70),
                ConfidenceLevel.HIGH, "Strong research aptitude", 2,
                OffsetDateTime.now().plusMonths(3)
        );

        when(careerRecommendationRepository.findActiveByStudentId(studentId))
                .thenReturn(List.of(active1, active2));

        List<CareerRecommendationResponse> result = service.getActiveRecommendations(studentId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).isActive()).isTrue();
        assertThat(result.get(1).isActive()).isTrue();
        assertThat(result.get(0).careerStream()).isEqualTo(CareerStream.ENGINEERING);
        assertThat(result.get(1).careerStream()).isEqualTo(CareerStream.RESEARCH);
    }
}
