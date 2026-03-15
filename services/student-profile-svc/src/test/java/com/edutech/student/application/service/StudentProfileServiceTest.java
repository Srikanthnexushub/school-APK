package com.edutech.student.application.service;

import com.edutech.student.application.dto.CreateStudentProfileRequest;
import com.edutech.student.application.dto.StudentProfileResponse;
import com.edutech.student.application.dto.UpdateStudentProfileRequest;
import com.edutech.student.application.exception.DuplicateStudentException;
import com.edutech.student.application.exception.StudentNotFoundException;
import com.edutech.student.domain.model.Board;
import com.edutech.student.domain.model.Gender;
import com.edutech.student.domain.model.ProfileStatus;
import com.edutech.student.domain.model.StudentProfile;
import com.edutech.student.domain.model.Stream;
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
import org.slf4j.Logger;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StudentProfileService Unit Tests")
class StudentProfileServiceTest {

    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();

    @Mock
    private StudentProfileRepository profileRepository;

    @Mock
    private StudentEventPublisher eventPublisher;

    @Mock
    private Logger log;

    @InjectMocks
    private StudentProfileService studentProfileService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private CreateStudentProfileRequest buildCreateRequest() {
        return new CreateStudentProfileRequest(
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
    }

    private StudentProfile buildSavedProfile() {
        return StudentProfile.create(
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
    }

    // -------------------------------------------------------------------------
    // createProfile tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createProfile_success: saves profile, publishes event, returns response")
    void createProfile_success() {
        // arrange
        CreateStudentProfileRequest request = buildCreateRequest();
        StudentProfile savedProfile = buildSavedProfile();

        when(profileRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(profileRepository.save(any(StudentProfile.class))).thenReturn(savedProfile);

        // act
        StudentProfileResponse response = studentProfileService.createProfile(request);

        // assert
        assertThat(response).isNotNull();
        assertThat(response.firstName()).isEqualTo("Arjun");
        assertThat(response.lastName()).isEqualTo("Sharma");
        assertThat(response.email()).isEqualTo("arjun@test.com");
        assertThat(response.status()).isEqualTo(ProfileStatus.ACTIVE);
        verify(profileRepository).save(any(StudentProfile.class));
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("createProfile_duplicateEmail: throws DuplicateStudentException")
    void createProfile_duplicateEmail() {
        // arrange
        CreateStudentProfileRequest request = buildCreateRequest();
        StudentProfile existingProfile = buildSavedProfile();

        when(profileRepository.findByEmail(request.email())).thenReturn(Optional.of(existingProfile));

        // act & assert
        assertThatThrownBy(() -> studentProfileService.createProfile(request))
                .isInstanceOf(DuplicateStudentException.class)
                .hasMessageContaining("arjun@test.com");

        verify(profileRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    // -------------------------------------------------------------------------
    // getProfile tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getProfile_found: returns StudentProfileResponse")
    void getProfile_found() {
        // arrange
        StudentProfile profile = buildSavedProfile();
        when(profileRepository.findById(STUDENT_ID)).thenReturn(Optional.of(profile));

        // act
        StudentProfileResponse response = studentProfileService.getProfile(STUDENT_ID);

        // assert
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("arjun@test.com");
        assertThat(response.firstName()).isEqualTo("Arjun");
    }

    @Test
    @DisplayName("getProfile_notFound: throws StudentNotFoundException")
    void getProfile_notFound() {
        // arrange
        when(profileRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> studentProfileService.getProfile(STUDENT_ID))
                .isInstanceOf(StudentNotFoundException.class)
                .hasMessageContaining(STUDENT_ID.toString());
    }

    // -------------------------------------------------------------------------
    // updateProfile tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updateProfile_success: updates allowed fields, saves, returns response")
    void updateProfile_success() {
        // arrange
        StudentProfile profile = buildSavedProfile();
        UpdateStudentProfileRequest request = new UpdateStudentProfileRequest(
                null,
                null,
                "8888888888",
                null,
                "Mumbai",
                "Maharashtra",
                Stream.PCM,
                2026
        );

        when(profileRepository.findById(STUDENT_ID)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(StudentProfile.class))).thenReturn(profile);

        // act
        StudentProfileResponse response = studentProfileService.updateProfile(STUDENT_ID, request);

        // assert
        assertThat(response).isNotNull();
        verify(profileRepository).save(any(StudentProfile.class));
    }

    @Test
    @DisplayName("updateProfile_notFound: throws StudentNotFoundException when student does not exist")
    void updateProfile_notFound() {
        // arrange
        UpdateStudentProfileRequest request = new UpdateStudentProfileRequest(
                null, null, "8888888888", null, null, null, null, null
        );
        when(profileRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> studentProfileService.updateProfile(STUDENT_ID, request))
                .isInstanceOf(StudentNotFoundException.class);

        verify(profileRepository, never()).save(any());
    }
}
