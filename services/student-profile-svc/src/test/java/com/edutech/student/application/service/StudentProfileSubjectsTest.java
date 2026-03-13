package com.edutech.student.application.service;

import com.edutech.student.application.dto.CreateStudentProfileRequest;
import com.edutech.student.application.dto.StudentProfileResponse;
import com.edutech.student.domain.model.Board;
import com.edutech.student.domain.model.Gender;
import com.edutech.student.domain.model.StudentProfile;
import com.edutech.student.domain.port.out.StudentEventPublisher;
import com.edutech.student.domain.port.out.StudentProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StudentProfileService — subjects field tests")
class StudentProfileSubjectsTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private StudentProfileRepository profileRepository;

    @Mock
    private StudentEventPublisher eventPublisher;

    @InjectMocks
    private StudentProfileService studentProfileService;

    private StudentProfile buildSavedProfile(List<String> subjects) {
        StudentProfile profile = StudentProfile.create(
                USER_ID,
                "Arjun",
                "Sharma",
                "arjun@test.com",
                "9999999999",
                Gender.MALE,
                LocalDate.of(2005, 3, 15),
                "Delhi",
                "Delhi",
                "110001",
                Board.CBSE,
                11
        );
        profile.setSubjects(subjects);
        return profile;
    }

    @Test
    @DisplayName("createProfile_withSubjects_persistsSubjectsAndReturnsThemInResponse")
    void createProfile_withSubjects_persistsSubjectsAndReturnsThemInResponse() {
        // Arrange
        List<String> subjectList = List.of("Mathematics", "Science");
        CreateStudentProfileRequest request = new CreateStudentProfileRequest(
                USER_ID,
                "Arjun",
                "Sharma",
                "arjun@test.com",
                "9999999999",
                Gender.MALE,
                LocalDate.of(2005, 3, 15),
                "Delhi",
                "Delhi",
                "110001",
                Board.CBSE,
                11,
                subjectList
        );

        StudentProfile savedProfile = buildSavedProfile(subjectList);

        when(profileRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(profileRepository.save(any(StudentProfile.class))).thenReturn(savedProfile);
        doNothing().when(eventPublisher).publish(any());

        // Act
        StudentProfileResponse response = studentProfileService.createProfile(request);

        // Assert
        assertThat(response.subjects()).isNotNull();
        assertThat(response.subjects()).contains("Mathematics", "Science");
    }

    @Test
    @DisplayName("createProfile_withNullSubjects_persistsEmptyList")
    void createProfile_withNullSubjects_persistsEmptyList() {
        // Arrange
        CreateStudentProfileRequest request = new CreateStudentProfileRequest(
                USER_ID,
                "Arjun",
                "Sharma",
                "arjun@test.com",
                "9999999999",
                Gender.MALE,
                LocalDate.of(2005, 3, 15),
                "Delhi",
                "Delhi",
                "110001",
                Board.CBSE,
                11,
                null
        );

        StudentProfile savedProfile = buildSavedProfile(List.of());

        when(profileRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(profileRepository.save(any(StudentProfile.class))).thenReturn(savedProfile);
        doNothing().when(eventPublisher).publish(any());

        // Act
        StudentProfileResponse response = studentProfileService.createProfile(request);

        // Assert
        assertThat(response.subjects()).isNotNull();
        assertThat(response.subjects()).isEmpty();
    }
}
