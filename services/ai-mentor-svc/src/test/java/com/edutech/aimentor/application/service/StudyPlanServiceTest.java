package com.edutech.aimentor.application.service;

import com.edutech.aimentor.application.dto.CreateStudyPlanRequest;
import com.edutech.aimentor.application.dto.StudyPlanItemResponse;
import com.edutech.aimentor.application.dto.StudyPlanResponse;
import com.edutech.aimentor.application.exception.StudyPlanNotFoundException;
import com.edutech.aimentor.domain.event.StudyPlanCreatedEvent;
import com.edutech.aimentor.domain.model.PriorityLevel;
import com.edutech.aimentor.domain.model.StudyPlan;
import com.edutech.aimentor.domain.model.StudyPlanItem;
import com.edutech.aimentor.domain.model.SubjectArea;
import com.edutech.aimentor.domain.port.out.AiMentorEventPublisher;
import com.edutech.aimentor.domain.port.out.StudyPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StudyPlanService unit tests")
class StudyPlanServiceTest {

    @Mock
    private StudyPlanRepository studyPlanRepository;

    @Mock
    private AiMentorEventPublisher eventPublisher;

    private StudyPlanService studyPlanService;

    @BeforeEach
    void setUp() {
        studyPlanService = new StudyPlanService(studyPlanRepository, eventPublisher);
    }

    @Test
    @DisplayName("createStudyPlan_success: saves plan and publishes StudyPlanCreatedEvent")
    void createStudyPlan_success() {
        // Given
        UUID studentId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        CreateStudyPlanRequest request = new CreateStudyPlanRequest(
                studentId, enrollmentId, "JEE Prep Plan", "Full JEE preparation",
                LocalDate.now().plusMonths(6), List.of()
        );

        StudyPlan savedPlan = StudyPlan.create(studentId, enrollmentId, "JEE Prep Plan",
                "Full JEE preparation", LocalDate.now().plusMonths(6));
        when(studyPlanRepository.save(any(StudyPlan.class))).thenReturn(savedPlan);

        // When
        studyPlanService.createStudyPlan(request);

        // Then
        verify(studyPlanRepository).save(any(StudyPlan.class));

        ArgumentCaptor<StudyPlanCreatedEvent> eventCaptor = ArgumentCaptor.forClass(StudyPlanCreatedEvent.class);
        verify(eventPublisher).publishStudyPlanCreated(eventCaptor.capture());

        StudyPlanCreatedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.studentId()).isEqualTo(studentId);
        assertThat(publishedEvent.enrollmentId()).isEqualTo(enrollmentId);
        assertThat(publishedEvent.title()).isEqualTo("JEE Prep Plan");
    }

    @Test
    @DisplayName("createStudyPlan_returnsResponse: returned StudyPlanResponse contains correct fields")
    void createStudyPlan_returnsResponse() {
        // Given
        UUID studentId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        LocalDate targetDate = LocalDate.now().plusMonths(3);
        CreateStudyPlanRequest request = new CreateStudyPlanRequest(
                studentId, enrollmentId, "NEET Prep", "NEET biology focus",
                targetDate, List.of()
        );

        StudyPlan savedPlan = StudyPlan.create(studentId, enrollmentId, "NEET Prep",
                "NEET biology focus", targetDate);
        when(studyPlanRepository.save(any(StudyPlan.class))).thenReturn(savedPlan);

        // When
        StudyPlanResponse response = studyPlanService.createStudyPlan(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.studentId()).isEqualTo(studentId);
        assertThat(response.enrollmentId()).isEqualTo(enrollmentId);
        assertThat(response.title()).isEqualTo("NEET Prep");
        assertThat(response.description()).isEqualTo("NEET biology focus");
        assertThat(response.targetExamDate()).isEqualTo(targetDate);
        assertThat(response.active()).isTrue();
        assertThat(response.items()).isEmpty();
    }

    @Test
    @DisplayName("getStudyPlan_found: returns StudyPlanResponse when plan exists for studentId+enrollmentId")
    void getStudyPlan_found() {
        // Given
        UUID studentId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        StudyPlan plan = StudyPlan.create(studentId, enrollmentId, "Physics Plan",
                "Mechanics focus", LocalDate.now().plusMonths(2));
        when(studyPlanRepository.findByStudentIdAndEnrollmentId(studentId, enrollmentId))
                .thenReturn(Optional.of(plan));

        // When
        StudyPlanResponse response = studyPlanService.getStudyPlan(studentId, enrollmentId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.studentId()).isEqualTo(studentId);
        assertThat(response.enrollmentId()).isEqualTo(enrollmentId);
        assertThat(response.title()).isEqualTo("Physics Plan");
    }

    @Test
    @DisplayName("getStudyPlan_notFound: throws StudyPlanNotFoundException when no plan found")
    void getStudyPlan_notFound() {
        // Given
        UUID studentId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        when(studyPlanRepository.findByStudentIdAndEnrollmentId(studentId, enrollmentId))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> studyPlanService.getStudyPlan(studentId, enrollmentId))
                .isInstanceOf(StudyPlanNotFoundException.class)
                .hasMessageContaining(studentId.toString());
    }

    @Test
    @DisplayName("updateStudyPlanItem_success: applies SM-2 review and saves item")
    void updateStudyPlanItem_success() {
        // Given
        UUID studentId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        StudyPlan plan = StudyPlan.create(studentId, enrollmentId, "Math Plan",
                "Calculus", LocalDate.now().plusMonths(4));
        StudyPlanItem item = StudyPlanItem.create(plan, SubjectArea.MATHEMATICS, "Calculus", PriorityLevel.HIGH);

        UUID itemId = item.getId();
        when(studyPlanRepository.findItemById(itemId)).thenReturn(Optional.of(item));
        when(studyPlanRepository.saveItem(any(StudyPlanItem.class))).thenReturn(item);

        // When
        StudyPlanItemResponse response = studyPlanService.reviewItem(itemId, studentId, 4);

        // Then
        verify(studyPlanRepository).saveItem(any(StudyPlanItem.class));
        assertThat(response).isNotNull();
        assertThat(response.quality()).isEqualTo(4);
        assertThat(response.repetitions()).isGreaterThan(0);
        assertThat(response.nextReviewAt()).isAfter(LocalDate.now());
    }
}
