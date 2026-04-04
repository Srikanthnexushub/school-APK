package com.edutech.careeroracle.application.service;

import com.edutech.careeroracle.application.dto.CareerRecommendationResponse;
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

import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
    void generateRecommendations_autoCreatesDefaultProfileWhenNotFound() {
        UUID unknownId = UUID.randomUUID();
        CareerProfile autoProfile = CareerProfile.create(
                unknownId, UUID.randomUUID(), "SCIENCE", 11,
                java.math.BigDecimal.valueOf(50), null);

        when(careerProfileRepository.findByStudentId(unknownId)).thenReturn(Optional.empty());
        when(careerProfileRepository.save(any(CareerProfile.class))).thenReturn(autoProfile);
        when(careerRecommendationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<CareerRecommendationResponse> result = service.generateRecommendations(unknownId, Map.of());

        assertThat(result).isNotEmpty().hasSize(CareerStream.values().length);
        verify(careerProfileRepository).save(any(CareerProfile.class));
        verify(careerRecommendationRepository).saveAll(anyList());
    }

    @Test
    void generateRecommendations_usesExistingProfileWhenFound() {
        when(careerProfileRepository.findByStudentId(studentId)).thenReturn(Optional.of(testProfile));
        when(careerRecommendationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        service.generateRecommendations(studentId, Map.of());

        // save() must NOT be called — existing profile was found
        verify(careerProfileRepository, org.mockito.Mockito.never()).save(any(CareerProfile.class));
    }

    // ─── Concurrent auto-create hardening ────────────────────────────────────

    @Test
    void generateRecommendations_concurrentAutoCreate_retriesOnDuplicateKeyViolation() {
        // Simulate: two simultaneous requests for the same new student.
        // First findByStudentId → empty (no profile). save() → DataIntegrityViolationException
        // (second thread already inserted). Retry findByStudentId → returns the profile.
        UUID unknownId = UUID.randomUUID();
        CareerProfile savedByOtherThread = CareerProfile.create(
                unknownId, UUID.randomUUID(), "SCIENCE", 11, BigDecimal.valueOf(50), null);

        when(careerProfileRepository.findByStudentId(unknownId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(savedByOtherThread));  // retry succeeds
        when(careerProfileRepository.save(any(CareerProfile.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));
        when(careerRecommendationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<CareerRecommendationResponse> result = service.generateRecommendations(unknownId, Map.of());

        assertThat(result).isNotEmpty().hasSize(CareerStream.values().length);
        verify(careerProfileRepository).save(any(CareerProfile.class));
    }

    @Test
    void generateRecommendations_concurrentAutoCreate_throwsWhenRetryAlsoFindsNothing() {
        // Edge case: save fails AND retry find also returns empty (extremely rare, profile was
        // deleted between save and retry). Should propagate as CareerProfileNotFoundException.
        UUID unknownId = UUID.randomUUID();

        when(careerProfileRepository.findByStudentId(unknownId)).thenReturn(Optional.empty());
        when(careerProfileRepository.save(any(CareerProfile.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.generateRecommendations(unknownId, Map.of()))
                .isInstanceOf(com.edutech.careeroracle.application.exception.CareerProfileNotFoundException.class);
    }

    // ─── Academic stream coverage ─────────────────────────────────────────────

    @Test
    void generateRecommendations_artsStream_producesValidScoresAndArtsLeadsLaw() {
        UUID artsId = UUID.randomUUID();
        CareerProfile artsProfile = CareerProfile.create(
                artsId, UUID.randomUUID(), "ARTS", 12, BigDecimal.valueOf(70), CareerStream.ARTS);

        when(careerProfileRepository.findByStudentId(artsId)).thenReturn(Optional.of(artsProfile));
        when(careerRecommendationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<CareerRecommendationResponse> result = service.generateRecommendations(artsId, Map.of());

        assertThat(result).hasSize(CareerStream.values().length);
        // All scores must be in [0, 100]
        result.forEach(r -> assertThat(r.fitScore())
                .isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100)));
        // ARTS stream student: ARTS career score should be higher than ENGINEERING
        BigDecimal artsScore = scoreFor(result, CareerStream.ARTS);
        BigDecimal engScore  = scoreFor(result, CareerStream.ENGINEERING);
        assertThat(artsScore).isGreaterThan(engScore);
        // LAW also gets a bonus for ARTS stream
        BigDecimal lawScore = scoreFor(result, CareerStream.LAW);
        assertThat(lawScore).isGreaterThan(BigDecimal.valueOf(40));
    }

    @Test
    void generateRecommendations_commerceStream_commerceAndManagementLeadEngineering() {
        UUID comId = UUID.randomUUID();
        CareerProfile comProfile = CareerProfile.create(
                comId, UUID.randomUUID(), "COMMERCE", 11, BigDecimal.valueOf(72), CareerStream.MANAGEMENT);

        when(careerProfileRepository.findByStudentId(comId)).thenReturn(Optional.of(comProfile));
        when(careerRecommendationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, BigDecimal> strengths = Map.of(
                "MATHS", BigDecimal.valueOf(80), "ECONOMICS", BigDecimal.valueOf(85));
        List<CareerRecommendationResponse> result = service.generateRecommendations(comId, strengths);

        assertThat(result).hasSize(CareerStream.values().length);
        result.forEach(r -> assertThat(r.fitScore()).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100)));
        // COMMERCE student: COMMERCE and MANAGEMENT should outrank ENGINEERING
        BigDecimal comScore  = scoreFor(result, CareerStream.COMMERCE);
        BigDecimal mgmtScore = scoreFor(result, CareerStream.MANAGEMENT);
        BigDecimal engScore  = scoreFor(result, CareerStream.ENGINEERING);
        assertThat(comScore).isGreaterThan(engScore);
        assertThat(mgmtScore).isGreaterThan(engScore);
    }

    @Test
    void generateRecommendations_nullErsScore_usesZeroAndProducesAllStreams() {
        UUID nullErsId = UUID.randomUUID();
        // ersScore = null — CareerScoreCalculator defaults to BigDecimal.ZERO
        CareerProfile nullErsProfile = CareerProfile.create(
                nullErsId, UUID.randomUUID(), "SCIENCE", 10, null, null);

        when(careerProfileRepository.findByStudentId(nullErsId)).thenReturn(Optional.of(nullErsProfile));
        when(careerRecommendationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<CareerRecommendationResponse> result = service.generateRecommendations(nullErsId, Map.of());

        assertThat(result).hasSize(CareerStream.values().length);
        result.forEach(r -> assertThat(r.fitScore()).isGreaterThanOrEqualTo(BigDecimal.ZERO));
    }

    // ─── Query hardening ──────────────────────────────────────────────────────

    @Test
    void getActiveRecommendations_returnsEmptyList_whenNoneExist() {
        when(careerRecommendationRepository.findActiveByStudentId(studentId)).thenReturn(List.of());
        List<CareerRecommendationResponse> result = service.getActiveRecommendations(studentId);
        assertThat(result).isEmpty();
    }

    @Test
    void generateRecommendations_deactivatesPreviousBeforeSavingNew() {
        when(careerProfileRepository.findByStudentId(studentId)).thenReturn(Optional.of(testProfile));
        when(careerRecommendationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        service.generateRecommendations(studentId, Map.of());

        // deactivate must be called BEFORE saveAll
        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(
                careerRecommendationRepository, careerRecommendationRepository);
        inOrder.verify(careerRecommendationRepository).deactivateAllByStudentId(studentId);
        inOrder.verify(careerRecommendationRepository).saveAll(anyList());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private BigDecimal scoreFor(List<CareerRecommendationResponse> recs, CareerStream stream) {
        return recs.stream()
                .filter(r -> r.careerStream() == stream)
                .findFirst()
                .map(CareerRecommendationResponse::fitScore)
                .orElseThrow(() -> new AssertionError("Missing stream: " + stream));
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
