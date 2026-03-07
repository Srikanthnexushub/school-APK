package com.edutech.performance.application.service;

import com.edutech.performance.application.dto.RecordWeakAreaRequest;
import com.edutech.performance.application.dto.WeakAreaResponse;
import com.edutech.performance.domain.model.ErrorType;
import com.edutech.performance.domain.model.WeakAreaRecord;
import com.edutech.performance.domain.port.out.PerformanceEventPublisher;
import com.edutech.performance.domain.port.out.WeakAreaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WeakAreaService Unit Tests")
class WeakAreaServiceTest {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();

    @Mock
    WeakAreaRepository weakAreaRepository;
    @Mock
    PerformanceEventPublisher eventPublisher;

    @InjectMocks
    WeakAreaService weakAreaService;

    private RecordWeakAreaRequest buildRequest(BigDecimal masteryPercent) {
        return new RecordWeakAreaRequest(
                ENROLLMENT_ID,
                "Mathematics",
                "Calculus",
                "Derivatives",
                masteryPercent,
                ErrorType.CONCEPTUAL_GAP,
                5,
                10,
                false
        );
    }

    @Test
    @DisplayName("recordWeakArea_belowThreshold: mastery < 60 -> creates WeakAreaRecord and publishes event")
    void recordWeakArea_belowThreshold() {
        RecordWeakAreaRequest request = buildRequest(new BigDecimal("45.00"));
        when(weakAreaRepository.save(any(WeakAreaRecord.class))).thenAnswer(i -> i.getArgument(0));

        WeakAreaResponse response = weakAreaService.recordWeakArea(STUDENT_ID, request);

        assertThat(response).isNotNull();
        assertThat(response.masteryPercent()).isEqualByComparingTo(new BigDecimal("45.00"));
        assertThat(response.subject()).isEqualTo("Mathematics");
        verify(weakAreaRepository).save(any(WeakAreaRecord.class));
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("recordWeakArea_aboveThreshold: mastery >= 60 -> saves record but does NOT publish event")
    void recordWeakArea_aboveThreshold() {
        RecordWeakAreaRequest request = buildRequest(new BigDecimal("75.00"));
        when(weakAreaRepository.save(any(WeakAreaRecord.class))).thenAnswer(i -> i.getArgument(0));

        WeakAreaResponse response = weakAreaService.recordWeakArea(STUDENT_ID, request);

        assertThat(response).isNotNull();
        assertThat(response.masteryPercent()).isEqualByComparingTo(new BigDecimal("75.00"));
        verify(weakAreaRepository).save(any(WeakAreaRecord.class));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("getTopWeakAreas_returnsOrdered: returns sorted by masteryPercent ascending")
    void getTopWeakAreas_returnsOrdered() {
        WeakAreaRecord r1 = WeakAreaRecord.detect(STUDENT_ID, ENROLLMENT_ID, "Physics", "Optics",
                new BigDecimal("20.00"), ErrorType.TOPIC_UNFAMILIARITY);
        WeakAreaRecord r2 = WeakAreaRecord.detect(STUDENT_ID, ENROLLMENT_ID, "Chemistry", "Bonds",
                new BigDecimal("35.00"), ErrorType.CONCEPTUAL_GAP);
        WeakAreaRecord r3 = WeakAreaRecord.detect(STUDENT_ID, ENROLLMENT_ID, "Math", "Algebra",
                new BigDecimal("10.00"), ErrorType.CALCULATION_ERROR);

        when(weakAreaRepository.findByStudentIdAndEnrollmentId(STUDENT_ID, ENROLLMENT_ID))
                .thenReturn(List.of(r1, r2, r3));

        List<WeakAreaResponse> result = weakAreaService.getTopWeakAreas(STUDENT_ID, ENROLLMENT_ID, 3);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).masteryPercent()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(result.get(1).masteryPercent()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(result.get(2).masteryPercent()).isEqualByComparingTo(new BigDecimal("35.00"));
    }
}
