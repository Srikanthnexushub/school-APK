package com.edutech.student.application.service;

import com.edutech.student.application.dto.SetTargetExamRequest;
import com.edutech.student.application.dto.TargetExamResponse;
import com.edutech.student.domain.model.Board;
import com.edutech.student.domain.model.ExamCode;
import com.edutech.student.domain.model.Gender;
import com.edutech.student.domain.model.StudentProfile;
import com.edutech.student.domain.model.TargetExam;
import com.edutech.student.domain.port.out.StudentEventPublisher;
import com.edutech.student.domain.port.out.StudentProfileRepository;
import com.edutech.student.domain.port.out.TargetExamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TargetExamService Unit Tests")
class TargetExamServiceTest {

    private static final UUID STUDENT_ID = UUID.randomUUID();

    @Mock
    private StudentProfileRepository profileRepository;

    @Mock
    private TargetExamRepository targetExamRepository;

    @Mock
    private StudentEventPublisher eventPublisher;

    @Mock
    private Logger log;

    @InjectMocks
    private TargetExamService targetExamService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private StudentProfile buildStudentProfile() {
        return StudentProfile.create(
                UUID.randomUUID(),
                "Priya",
                "Nair",
                "priya@test.com",
                "7777777777",
                Gender.FEMALE,
                LocalDate.of(2006, 5, 20),
                "Chennai",
                "Tamil Nadu",
                "600001",
                Board.CBSE,
                12
        );
    }

    private SetTargetExamRequest buildRequest(ExamCode code, Integer year, Integer priority) {
        return new SetTargetExamRequest(code, year, priority);
    }

    // -------------------------------------------------------------------------
    // setTargetExam tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("setTargetExam_success: creates target exam, publishes event")
    void setTargetExam_success() {
        // arrange
        StudentProfile student = buildStudentProfile();
        SetTargetExamRequest request = buildRequest(ExamCode.JEE_MAIN, 2026, 1);

        when(profileRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(targetExamRepository.findByStudentId(STUDENT_ID)).thenReturn(Collections.emptyList());
        when(targetExamRepository.save(any(TargetExam.class))).thenAnswer(i -> i.getArgument(0));

        // act
        TargetExamResponse response = targetExamService.setTargetExam(STUDENT_ID, request);

        // assert
        assertThat(response).isNotNull();
        assertThat(response.examCode()).isEqualTo(ExamCode.JEE_MAIN);
        assertThat(response.targetYear()).isEqualTo(2026);
        assertThat(response.priority()).isEqualTo(1);
        verify(targetExamRepository).save(any(TargetExam.class));
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("setTargetExam_replaceExisting: soft-deletes old, creates new for same examCode")
    void setTargetExam_replaceExisting() {
        // arrange
        StudentProfile student = buildStudentProfile();
        SetTargetExamRequest request = buildRequest(ExamCode.JEE_MAIN, 2027, 1);

        TargetExam existingExam = TargetExam.create(STUDENT_ID, ExamCode.JEE_MAIN, 2026, 1);

        when(profileRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(targetExamRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of(existingExam));
        when(targetExamRepository.save(any(TargetExam.class))).thenAnswer(i -> i.getArgument(0));

        // act
        TargetExamResponse response = targetExamService.setTargetExam(STUDENT_ID, request);

        // assert - save called twice: once to soft-delete old, once to save new
        verify(targetExamRepository, times(2)).save(any(TargetExam.class));
        assertThat(existingExam.getDeletedAt()).isNotNull();
        assertThat(response.targetYear()).isEqualTo(2027);
        verify(eventPublisher).publish(any());
    }
}
